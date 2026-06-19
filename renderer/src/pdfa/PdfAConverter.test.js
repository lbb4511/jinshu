const { describe, it, before, after } = require('node:test');
const assert = require('node:assert');
const fs = require('fs');
const path = require('path');
const os = require('os');
const { PdfAConverter } = require('./PdfAConverter');

describe('PdfAConverter', () => {
  const converter = new PdfAConverter({ logger: console });

  it('should throw when input file does not exist', async () => {
    await assert.rejects(
      () => converter.convert('/nonexistent/input.pdf', '/nonexistent/output.pdf'),
      /Input file not found/
    );
  });

  it('should return fallback input path when ghostscript is not available', async () => {
    const inputPath = path.join(os.tmpdir(), `test-pdfa-input-${Date.now()}.pdf`);
    const outputPath = path.join(os.tmpdir(), `test-pdfa-output-${Date.now()}.pdf`);

    before(() => {
      fs.writeFileSync(inputPath, '%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<< /Size 1 /Root 1 0 R >>\n%%EOF');
    });

    const localConverter = new PdfAConverter({ logger: console, ghostscriptPath: 'nonexistent_gs_binary' });
    const result = await localConverter.convert(inputPath, outputPath, 'PDF/A-2b');

    assert.strictEqual(result.outputPath, inputPath);
    assert.ok(result.warning);

    after(() => {
      try {
        fs.unlinkSync(inputPath);
        if (fs.existsSync(outputPath)) fs.unlinkSync(outputPath);
      } catch (e) { /* ignore */ }
    });
  });
});
