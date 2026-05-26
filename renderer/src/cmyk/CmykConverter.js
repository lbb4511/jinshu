const { execFile } = require('child_process');
const fs = require('fs');

class CmykConverter {
  constructor(options = {}) {
    this.logger = options.logger || console;
    this.ghostscriptPath = options.ghostscriptPath || 'gs';
    this.timeout = options.timeout || 120000;
  }

  async convert(inputPath, outputPath) {
    if (!fs.existsSync(inputPath)) {
      throw new Error(`Input file not found: ${inputPath}`);
    }

    const outputDir = require('path').dirname(outputPath);
    fs.mkdirSync(outputDir, { recursive: true });

    const args = [
      '-dNOPAUSE',
      '-dBATCH',
      '-dQUIET',
      '-sDEVICE=pdfwrite',
      '-sProcessColorModel=DeviceCMYK',
      '-sColorConversionStrategy=CMYK',
      '-sColorConversionStrategyForImages=CMYK',
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

    this.logger.info(`Converting to CMYK: ${inputPath} -> ${outputPath}`);

    try {
      const { stdout, stderr } = await this._exec(this.ghostscriptPath, args, this.timeout);

      if (!fs.existsSync(outputPath)) {
        throw new Error('CMYK output file was not created');
      }

      const size = fs.statSync(outputPath).size;
      this.logger.info(`CMYK conversion completed: ${outputPath}, size=${size}`);
      return { outputPath, size };

    } catch (err) {
      this.logger.warn(`CMYK conversion failed, will keep RGB version: ${err.message}`);
      return { outputPath: inputPath, size: fs.statSync(inputPath).size, warning: 'CMYK conversion failed' };
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

module.exports = { CmykConverter };
