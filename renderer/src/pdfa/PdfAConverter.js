const { execFile } = require('child_process');
const fs = require('fs');
const path = require('path');

class PdfAConverter {
  constructor(options = {}) {
    this.logger = options.logger || console;
    this.ghostscriptPath = options.ghostscriptPath || 'gs';
    this.timeout = options.timeout || 120000;
  }

  async convert(inputPath, outputPath, level = 'PDF/A-2b') {
    if (!fs.existsSync(inputPath)) {
      throw new Error(`Input file not found: ${inputPath}`);
    }

    const outputDir = path.dirname(outputPath);
    fs.mkdirSync(outputDir, { recursive: true });

    const pdfaLevel = level === 'PDF/A-1b' ? '1' : '2';

    const args = [
      '-dNOPAUSE',
      '-dBATCH',
      '-dQUIET',
      '-sDEVICE=pdfwrite',
      `-dPDFA=${pdfaLevel}`,
      '-dPDFACompatibilityPolicy=1',
      '-sColorConversionStrategy=UseDeviceIndependentColor',
      '-sProcessColorModel=DeviceRGB',
      '-dDownsampleMonoImages=false',
      '-dDownsampleGrayImages=false',
      '-dDownsampleColorImages=false',
      '-dAutoFilterColorImages=false',
      '-dAutoFilterGrayImages=false',
      '-dColorImageFilter=/FlateEncode',
      '-dGrayImageFilter=/FlateEncode',
      `-sOutputFile=${outputPath}`,
      inputPath,
    ];

    this.logger.info(`Converting to ${level}: ${inputPath} -> ${outputPath}`);

    try {
      const { stdout, stderr } = await this._exec(this.ghostscriptPath, args, this.timeout);

      if (!fs.existsSync(outputPath)) {
        throw new Error('PDF/A output file was not created');
      }

      const size = fs.statSync(outputPath).size;
      this.logger.info(`PDF/A conversion completed: ${outputPath}, size=${size}`);
      return { outputPath, size, level };

    } catch (err) {
      this.logger.warn(`PDF/A conversion failed, will keep original version: ${err.message}`);
      return { outputPath: inputPath, size: fs.statSync(inputPath).size, warning: 'PDF/A conversion failed' };
    }
  }

  _exec(command, args, timeout) {
    return new Promise((resolve, reject) => {
      const child = execFile(command, args, { timeout }, (err, stdout, stderr) => {
        if (err) {
          reject(new Error(`Ghostscript failed: ${err.message}\nstderr: ${stderr}`));
        } else {
          resolve({ stdout, stderr });
        }
      });
    });
  }
}

module.exports = { PdfAConverter };
