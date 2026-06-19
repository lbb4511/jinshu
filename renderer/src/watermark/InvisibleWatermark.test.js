const { describe, it, before, after } = require('node:test');
const assert = require('node:assert');
const fs = require('fs');
const path = require('path');
const os = require('os');
const { InvisibleWatermark } = require('./InvisibleWatermark');

describe('InvisibleWatermark', () => {
  const watermark = new InvisibleWatermark({ logger: console });

  it('should produce consistent hex hash', () => {
    const encoded = watermark.encode('user-1', 123456789);
    assert.strictEqual(encoded.length, 64);
    assert.strictEqual(watermark.verify(encoded, 'user-1', 123456789), true);
    assert.strictEqual(watermark.verify(encoded, 'user-2', 123456789), false);
  });

  it('should append watermark comment to PDF', async () => {
    const inputPath = path.join(os.tmpdir(), `test-input-${Date.now()}.pdf`);
    const outputPath = path.join(os.tmpdir(), `test-output-${Date.now()}.pdf`);
    before(() => {
      fs.writeFileSync(inputPath, '%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<< /Size 1 /Root 1 0 R >>\n%%EOF');
    });

    const result = await watermark.embedIntoPdf(inputPath, outputPath, 'user-42');

    assert.strictEqual(result.outputPath, outputPath);
    assert.strictEqual(fs.existsSync(outputPath), true);
    const content = fs.readFileSync(outputPath, 'utf8');
    assert.ok(content.includes('%%JinshuWatermark:'));
    assert.ok(content.includes('userId=user-42'));
    assert.ok(content.includes('%%EOF'));

    after(() => {
      try {
        fs.unlinkSync(inputPath);
        fs.unlinkSync(outputPath);
      } catch (e) { /* ignore */ }
    });
  });
});
