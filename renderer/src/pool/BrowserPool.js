const puppeteer = require('puppeteer');

class BrowserPool {
  constructor(options = {}) {
    this.logger = options.logger || console;
    this.min = options.min || 2;
    this.max = options.max || 10;
    this.idleTimeout = options.idleTimeout || 30000;
    this.maxUses = options.maxUses || 50;
    this.acquireTimeout = options.acquireTimeout || 10000;

    this._instances = [];
    this._queue = [];
    this._isDraining = false;

    for (let i = 0; i < this.min; i++) {
      this._createInstance();
    }

    this._idleCheckInterval = setInterval(() => this._reapIdle(), this.idleTimeout);
  }

  async acquire() {
    if (this._isDraining) {
      throw new Error('Browser pool is draining');
    }

    const instance = this._getIdleInstance();
    if (instance) {
      instance.useCount++;
      return instance;
    }

    if (this._instances.length < this.max) {
      const newInstance = await this._createInstance();
      newInstance.useCount++;
      return newInstance;
    }

    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        const idx = this._queue.findIndex(item => item.resolve === resolve);
        if (idx !== -1) this._queue.splice(idx, 1);
        reject(new Error('Acquire timeout'));
      }, this.acquireTimeout);

      this._queue.push({ resolve, reject, timeout });
    });
  }

  async release(instance) {
    if (instance.useCount >= this.maxUses) {
      this.logger.info(`Browser instance reached max uses (${this.maxUses}), restarting`);
      await this._destroyInstance(instance);
      this._createInstance();
      return;
    }

    instance.busy = false;
    instance.lastUsed = Date.now();

    if (this._queue.length > 0) {
      const next = this._queue.shift();
      clearTimeout(next.timeout);
      instance.busy = true;
      instance.useCount++;
      next.resolve(instance);
    }
  }

  async drain() {
    this._isDraining = true;
    clearInterval(this._idleCheckInterval);

    for (const item of this._queue) {
      clearTimeout(item.timeout);
      item.reject(new Error('Pool is draining'));
    }
    this._queue = [];

    for (const instance of this._instances) {
      await this._destroyInstance(instance);
    }
    this._instances = [];
  }

  getStats() {
    const total = this._instances.length;
    const busy = this._instances.filter(i => i.busy).length;
    const idle = total - busy;
    return { total, busy, idle, queued: this._queue.length };
  }

  async _createInstance() {
    try {
      const browser = await puppeteer.launch({
        headless: 'new',
        args: [
          '--no-sandbox',
          '--disable-setuid-sandbox',
          '--disable-dev-shm-usage',
          '--disable-accelerated-2d-canvas',
          '--no-first-run',
          '--no-zygote',
          '--single-process',
          '--disable-gpu',
          '--js-flags=--max-old-space-size=512',
          '--max-old-space-size=512',
        ],
      });

      const instance = {
        browser,
        useCount: 0,
        busy: false,
        lastUsed: Date.now(),
        createdAt: Date.now(),
        pagePool: [],
      };

      this._instances.push(instance);
      this.logger.info(`Created browser instance (total: ${this._instances.length})`);
      return instance;
    } catch (err) {
      this.logger.error('Failed to create browser instance', err);
      throw err;
    }
  }

  async _destroyInstance(instance) {
    try {
      for (const page of instance.pagePool) {
        try { await page.close(); } catch (e) { /* ignore */ }
      }
      await instance.browser.close();
    } catch (err) {
      this.logger.warn('Error closing browser', err);
    }

    const idx = this._instances.indexOf(instance);
    if (idx !== -1) this._instances.splice(idx, 1);
  }

  _getIdleInstance() {
    const idle = this._instances.filter(i => !i.busy);
    if (idle.length === 0) return null;

    idle.sort((a, b) => a.lastUsed - b.lastUsed);
    return idle[0];
  }

  _reapIdle() {
    const now = Date.now();
    const target = this._instances.length - this.min;

    if (target <= 0) return;

    const idle = this._instances.filter(i => !i.busy && (now - i.lastUsed) > this.idleTimeout);
    idle.sort((a, b) => a.lastUsed - b.lastUsed);

    const toRemove = idle.slice(0, target);
    for (const instance of toRemove) {
      this._destroyInstance(instance);
    }

    if (toRemove.length > 0) {
      this.logger.info(`Reaped ${toRemove.length} idle instances (total: ${this._instances.length})`);
    }
  }
}

module.exports = { BrowserPool };
