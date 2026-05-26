const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

class PdfRenderer {
  constructor(options = {}) {
    this.browserPool = options.browserPool;
    this.logger = options.logger || console;
    this.timeout = options.timeout || 60000;
  }

  async render(config) {
    const taskId = config.parentTaskId;
    const seq = config.seq;
    const renderUrl = config.renderUrl;
    const pageFrom = config.pageFrom || 1;
    const pageTo = config.pageTo || 1;

    if (!renderUrl) {
      throw new Error('renderUrl is required');
    }

    const outputDir = `/data/temp/${taskId}`;
    fs.mkdirSync(outputDir, { recursive: true });

    const outputPath = path.join(outputDir, `segment_${String(seq).padStart(3, '0')}.pdf`);

    const instance = await this.browserPool.acquire();
    let page = null;

    try {
      page = await instance.browser.newPage();

      await page.setViewport({ width: 1240, height: 1754 });

      this.logger.info(`Navigating to: ${renderUrl} (pages ${pageFrom}-${pageTo})`);
      await page.goto(renderUrl, {
        waitUntil: 'networkidle2',
        timeout: 30000,
      });

      await page.waitForFunction(() => {
        const charts = document.querySelectorAll('.echarts-for-react, canvas');
        return charts.length > 0
          ? Array.from(charts).every(c => c.clientHeight > 0 && c.clientWidth > 0)
          : document.readyState === 'complete';
      }, { timeout: 15000 });

      await page.evaluate(() => {
        document.querySelectorAll('.navbar, .sidebar, .footer, .action-bar, .btn, button, a[href]')
          .forEach(el => {
            if (el.tagName === 'A' || el.tagName === 'BUTTON') {
              el.style.display = 'none';
            }
          });
        document.querySelectorAll('[data-sensitive]').forEach(el => {
          el.textContent = '***';
        });
      });

      this.logger.info(`Generating PDF: pages ${pageFrom}-${pageTo}`);
      const pdfBuffer = await page.pdf({
        format: 'A4',
        printBackground: true,
        preferCSSPageSize: true,
        margin: { top: '1cm', bottom: '1cm', left: '1cm', right: '1cm' },
        pageRanges: `${pageFrom}-${pageTo}`,
        displayHeaderFooter: true,
        headerTemplate: '<span></span>',
        footerTemplate: `
          <div style="font-size:10px;text-align:center;width:100%;">
            <span class="pageNumber"></span> / <span class="totalPages"></span>
          </div>
        `,
      });

      fs.writeFileSync(outputPath, pdfBuffer);
      this.logger.info(`PDF written: ${outputPath} (${pdfBuffer.length} bytes)`);

      const md5 = crypto.createHash('md5').update(pdfBuffer).digest('hex');

      return { taskId, seq, outputPath, md5, size: pdfBuffer.length };

    } catch (err) {
      this.logger.error(`Render failed: taskId=${taskId}, seq=${seq}`, err);
      throw err;
    } finally {
      if (page) {
        try { await page.close(); } catch (e) { /* ignore */ }
      }
      await this.browserPool.release(instance);
    }
  }
}

module.exports = { PdfRenderer };
