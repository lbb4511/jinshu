FROM node:22-slim

# Use Tsinghua mirror to speed up apt in CN environment
RUN sed -i 's/deb.debian.org/mirrors.tuna.tsinghua.edu.cn/g' /etc/apt/sources.list.d/debian.sources \
 && apt-get update \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    chromium \
    chromium-sandbox \
    fonts-noto \
    fonts-noto-cjk \
    ghostscript \
    openjdk-17-jre-headless \
 && rm -rf /var/lib/apt/lists/*

ENV PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium
ENV PDFBOX_JAR=/usr/share/java/pdfbox-app-3.0.7.jar

RUN mkdir -p /var/log/renderer /data/temp /data/output

WORKDIR /app

COPY package.json .
RUN npm install --production

COPY src/ src/

EXPOSE 3001

CMD ["node", "src/app.js"]
