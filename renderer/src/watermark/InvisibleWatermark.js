const crypto = require('crypto');

class InvisibleWatermark {
  constructor(options = {}) {
    this.logger = options.logger || console;
  }

  encode(userId, timestamp) {
    const payload = `${userId}|${timestamp}`;
    return crypto.createHash('sha256').update(payload).digest('hex');
  }

  verify(encoded, userId, timestamp) {
    const expected = this.encode(userId, timestamp);
    return crypto.timingSafeEqual(Buffer.from(encoded), Buffer.from(expected));
  }
}

module.exports = { InvisibleWatermark };
