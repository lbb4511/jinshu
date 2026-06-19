const crypto = require('crypto');
const fs = require('fs');

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

  /**
   * 将水印信息以非破坏方式追加到 PDF 文件末尾。
   * 实际生产环境建议使用 pdf-lib 等库写入标准 /Info 字典；
   * 本实现作为兜底方案，在无法引入外部库时保持水印可验证。
   */
  async embedIntoPdf(inputPath, outputPath, userId) {
    if (!fs.existsSync(inputPath)) {
      throw new Error(`Input file not found: ${inputPath}`);
    }

    const timestamp = Date.now();
    const payload = this.encode(userId, timestamp);
    const watermarkComment =
      `\n%%JinshuWatermark: userId=${userId}, timestamp=${timestamp}, hash=${payload}\n`;

    try {
      const data = fs.readFileSync(inputPath);
      const marker = Buffer.from('\n%%EOF');
      const lastEof = data.lastIndexOf(marker);
      let output;
      if (lastEof >= 0) {
        // 在最后一个 %%EOF 之前插入水印注释，保持 PDF 仍然以 %%EOF 结尾
        output = Buffer.concat([
          data.subarray(0, lastEof),
          Buffer.from(watermarkComment),
          data.subarray(lastEof),
        ]);
      } else {
        output = Buffer.concat([data, Buffer.from(watermarkComment)]);
      }
      fs.writeFileSync(outputPath, output);
      this.logger.info(`Invisible watermark embedded: output=${outputPath}, userId=${userId}`);
      return { outputPath, payload };
    } catch (err) {
      this.logger.error(`Failed to embed watermark: ${err.message}`);
      throw err;
    }
  }
}

module.exports = { InvisibleWatermark };
