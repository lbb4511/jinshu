const { execFile } = require('child_process');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

class PdfMerger {
  constructor(options = {}) {
    this.logger = options.logger || console;
    this.pdfboxJar = options.pdfboxJar || '/usr/share/java/pdfbox-app-3.0.7.jar';
    this.timeout = options.timeout || 60000;
  }

  async merge(inputFiles, outputPath) {
    if (!inputFiles || inputFiles.length === 0) {
      throw new Error('No input files to merge');
    }

    for (const file of inputFiles) {
      if (!fs.existsSync(file)) {
        throw new Error(`Input file not found: ${file}`);
      }
      const md5 = this._md5File(file);
      this.logger.info(`Segment file: ${file}, size=${fs.statSync(file).size}, md5=${md5}`);
    }

    const outputDir = path.dirname(outputPath);
    fs.mkdirSync(outputDir, { recursive: true });

    const args = [
      '-jar', this.pdfboxJar,
      'merge',
      '-o', outputPath,
      ...inputFiles,
    ];

    this.logger.info(`Merging ${inputFiles.length} PDFs into ${outputPath}`);

    try {
      const { stdout, stderr } = await this._exec('java', args, this.timeout);

      if (!fs.existsSync(outputPath)) {
        throw new Error('Output file was not created');
      }

      const totalSize = fs.statSync(outputPath).size;
      const totalMd5 = this._md5File(outputPath);
      this.logger.info(`Merge completed: ${outputPath}, size=${totalSize}, md5=${totalMd5}`);

      return { outputPath, size: totalSize, md5: totalMd5 };
    } catch (err) {
      this.logger.error(`PDF merge failed`, err);
      throw err;
    }
  }

  _exec(command, args, timeout) {
    return new Promise((resolve, reject) => {
      const child = execFile(command, args, { timeout }, (err, stdout, stderr) => {
        if (err) {
          reject(new Error(`Process failed: ${err.message}\nstderr: ${stderr}`));
        } else {
          resolve({ stdout, stderr });
        }
      });
    });
  }

  _md5File(filePath) {
    const data = fs.readFileSync(filePath);
    return crypto.createHash('md5').update(data).digest('hex');
  }
}

module.exports = { PdfMerger };
