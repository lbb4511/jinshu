package com.jinshu.api.controller;

import com.jinshu.api.service.FileDownloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * 文件下载控制器
 *
 * 提供任务结果文件下载，支持 HTTP Range 分片下载，
 * 可用于大文件断点续传、多线程下载及浏览器原生分片请求。
 */
@Slf4j
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileDownloadService fileDownloadService;

    private static final String BYTES_UNIT = "bytes";
    private static final String RANGE_PREFIX = "bytes=";

    /**
     * 下载任务结果文件，支持 HTTP Range 分片下载
     *
     * @param taskId      任务 ID
     * @param rangeHeader Range 请求头（可选）
     * @return 完整文件（200）或分片内容（206）
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> download(
            @RequestParam Long taskId,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) throws IOException {

        FileDownloadService.DownloadFile downloadFile = fileDownloadService.resolveDownloadFile(taskId);
        Path path = downloadFile.path();
        long fileSize = downloadFile.size();
        String fileName = downloadFile.fileName();
        String contentType = resolveContentType(path);

        if (!StringUtils.hasText(rangeHeader)) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.ACCEPT_RANGES, BYTES_UNIT)
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(fileName))
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(fileSize)
                    .body(new FileSystemResource(path));
        }

        return serveRange(path, fileSize, fileName, contentType, rangeHeader);
    }

    private ResponseEntity<Resource> serveRange(Path path, long fileSize, String fileName,
                                                String contentType, String rangeHeader) throws IOException {
        if (!rangeHeader.startsWith(RANGE_PREFIX)) {
            return rangeNotSatisfiable(fileSize);
        }

        String rangeValue = rangeHeader.substring(RANGE_PREFIX.length()).trim();
        // 仅支持单区间 Range，多区间返回 416 简化处理
        if (rangeValue.contains(",")) {
            return rangeNotSatisfiable(fileSize);
        }

        long start;
        long end;
        try {
            int dashIndex = rangeValue.indexOf('-');
            if (dashIndex < 0) {
                return rangeNotSatisfiable(fileSize);
            }
            String startPart = rangeValue.substring(0, dashIndex);
            String endPart = rangeValue.substring(dashIndex + 1);

            if (StringUtils.hasText(startPart)) {
                start = Long.parseLong(startPart);
                end = StringUtils.hasText(endPart) ? Long.parseLong(endPart) : fileSize - 1;
            } else {
                // suffix range: bytes=-500
                long suffixLength = Long.parseLong(endPart);
                start = Math.max(0, fileSize - suffixLength);
                end = fileSize - 1;
            }
        } catch (NumberFormatException e) {
            return rangeNotSatisfiable(fileSize);
        }

        if (start < 0 || end < 0 || start >= fileSize || end >= fileSize || start > end) {
            return rangeNotSatisfiable(fileSize);
        }

        long contentLength = end - start + 1;
        InputStream inputStream = java.nio.file.Files.newInputStream(path);
        long skipped = inputStream.skip(start);
        if (skipped < start) {
            inputStream.close();
            return rangeNotSatisfiable(fileSize);
        }
        InputStreamResource partialResource = new InputStreamResource(
                new BoundedInputStream(inputStream, contentLength));

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.ACCEPT_RANGES, BYTES_UNIT)
                .header(HttpHeaders.CONTENT_RANGE,
                        BYTES_UNIT + " " + start + "-" + end + "/" + fileSize)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(fileName))
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(contentLength)
                .body(partialResource);
    }

    private ResponseEntity<Resource> rangeNotSatisfiable(long fileSize) {
        return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                .header(HttpHeaders.CONTENT_RANGE, BYTES_UNIT + " */" + fileSize)
                .build();
    }

    private String contentDisposition(String fileName) {
        return "attachment; filename=\"" + fileName + "\"";
    }

    private String resolveContentType(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        if (fileName.endsWith(".csv")) {
            return "text/csv";
        }
        if (fileName.endsWith(".zip")) {
            return "application/zip";
        }
        if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    /**
     * 限定读取长度的输入流包装，用于返回 Range 分片
     */
    private static class BoundedInputStream extends InputStream {

        private final InputStream in;
        private long remaining;

        BoundedInputStream(InputStream in, long limit) {
            this.in = in;
            this.remaining = limit;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            remaining--;
            return in.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int toRead = (int) Math.min(len, remaining);
            int read = in.read(b, off, toRead);
            if (read > 0) {
                remaining -= read;
            }
            return read;
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }
}
