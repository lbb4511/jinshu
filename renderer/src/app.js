const amqp = require('amqplib');
const winston = require('winston');
const path = require('path');
const fs = require('fs');
const { BrowserPool } = require('./pool/BrowserPool');
const { PdfRenderer } = require('./render/PdfRenderer');
const { CmykConverter } = require('./cmyk/CmykConverter');
const { InvisibleWatermark } = require('./watermark/InvisibleWatermark');
const { PdfAConverter } = require('./pdfa/PdfAConverter');

const LOG_DIR = process.env.LOG_DIR || '/var/log/renderer';
if (!fs.existsSync(LOG_DIR)) {
  fs.mkdirSync(LOG_DIR, { recursive: true });
}

const logger = winston.createLogger({
  level: 'info',
  format: winston.format.combine(
    winston.format.timestamp(),
    winston.format.json()
  ),
  transports: [
    new winston.transports.Console({ format: winston.format.simple() }),
    new winston.transports.File({ filename: path.join(LOG_DIR, 'error.log'), level: 'error' }),
    new winston.transports.File({ filename: path.join(LOG_DIR, 'combined.log') })
  ]
});

const RABBITMQ_URL = process.env.RABBITMQ_URL || 'amqp://guest:guest@localhost:5672';
const SEGMENT_QUEUE = process.env.SEMENT_QUEUE || 'jinshu.render.segment';

async function main() {
  logger.info('Starting PDF renderer service...');

  const browserPool = new BrowserPool({ logger });
  const cmykConverter = new CmykConverter({ logger });
  const watermark = new InvisibleWatermark({ logger });
  const pdfaConverter = new PdfAConverter({ logger });
  const renderer = new PdfRenderer({ browserPool, cmykConverter, watermark, pdfaConverter, logger });

  let connection;
  try {
    connection = await amqp.connect(RABBITMQ_URL);
    const channel = await connection.createChannel();
    await channel.assertQueue(SEGMENT_QUEUE, { durable: true });
    channel.prefetch(1);

    logger.info(`Connected to RabbitMQ, consuming from queue: ${SEGMENT_QUEUE}`);

    channel.consume(SEGMENT_QUEUE, async (msg) => {
      if (!msg) return;

      const config = JSON.parse(msg.content.toString());
      logger.info(`Received segment task: taskId=${config.parentTaskId}, seq=${config.seq}`);

      try {
        const result = await renderer.render(config);
        logger.info(`Segment rendered: taskId=${config.parentTaskId}, seq=${config.seq}, output=${result.outputPath}`);
        channel.ack(msg);
      } catch (err) {
        logger.error(`Segment render failed: taskId=${config.parentTaskId}, seq=${config.seq}`, err);
        channel.nack(msg, false, true);
      }
    });

    process.on('SIGINT', async () => {
      logger.info('Shutting down renderer...');
      await browserPool.drain();
      await connection.close();
      process.exit(0);
    });

    process.on('SIGTERM', async () => {
      logger.info('Received SIGTERM, shutting down...');
      await browserPool.drain();
      await connection.close();
      process.exit(0);
    });

  } catch (err) {
    logger.error('Failed to start renderer', err);
    process.exit(1);
  }
}

main();
