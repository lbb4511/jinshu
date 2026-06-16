ARG BASE_IMAGE=gitea-http.gitea.svc.cluster.local:3000/admin/jinshu-renderer-base:latest
FROM ${BASE_IMAGE}

ENV PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium
ENV PDFBOX_JAR=/usr/share/java/pdfbox-app-3.0.7.jar

RUN mkdir -p /var/log/renderer /data/temp /data/output

WORKDIR /app

COPY package.json .
RUN npm install --production

COPY src/ src/

EXPOSE 3001

CMD ["node", "src/app.js"]
