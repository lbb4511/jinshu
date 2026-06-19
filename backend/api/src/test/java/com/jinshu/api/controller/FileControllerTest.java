package com.jinshu.api.controller;

import com.jinshu.api.service.FileDownloadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("FileController 文件下载控制器测试")
@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private FileDownloadService fileDownloadService;

    @InjectMocks
    private FileController fileController;

    private static final Long TASK_ID = 1L;

    @Test
    @DisplayName("GET /files/download 无 Range 返回 200 及完整文件")
    void given_noRangeHeader_when_download_then_return200WithFullFile() throws IOException {
        Path tempFile = Files.createTempFile("jinshu_full_", ".xlsx");
        Files.write(tempFile, "0123456789".getBytes());
        tempFile.toFile().deleteOnExit();

        when(fileDownloadService.resolveDownloadFile(TASK_ID))
                .thenReturn(new FileDownloadService.DownloadFile(tempFile, 10L, "report.xlsx"));

        ResponseEntity<Resource> response = fileController.download(TASK_ID, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCEPT_RANGES)).isEqualTo("bytes");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"report.xlsx\"");
        assertThat(response.getHeaders().getContentLength()).isEqualTo(10L);
        assertThat(response.getBody()).isNotNull();

        Files.deleteIfExists(tempFile);
    }

    @Test
    @DisplayName("GET /files/download 带 Range 返回 206 及分片内容")
    void given_validRangeHeader_when_download_then_return206WithPartialContent() throws IOException {
        Path tempFile = Files.createTempFile("jinshu_range_", ".csv");
        Files.write(tempFile, "0123456789".getBytes());
        tempFile.toFile().deleteOnExit();

        when(fileDownloadService.resolveDownloadFile(TASK_ID))
                .thenReturn(new FileDownloadService.DownloadFile(tempFile, 10L, "report.csv"));

        ResponseEntity<Resource> response = fileController.download(TASK_ID, "bytes=2-5");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes 2-5/10");
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCEPT_RANGES)).isEqualTo("bytes");
        assertThat(response.getHeaders().getContentLength()).isEqualTo(4L);
        assertThat(response.getBody()).isNotNull();

        byte[] body = response.getBody().getInputStream().readAllBytes();
        assertThat(new String(body)).isEqualTo("2345");

        Files.deleteIfExists(tempFile);
    }

    @Test
    @DisplayName("GET /files/download suffix Range 返回 206")
    void given_suffixRangeHeader_when_download_then_return206() throws IOException {
        Path tempFile = Files.createTempFile("jinshu_suffix_", ".zip");
        Files.write(tempFile, "0123456789".getBytes());
        tempFile.toFile().deleteOnExit();

        when(fileDownloadService.resolveDownloadFile(TASK_ID))
                .thenReturn(new FileDownloadService.DownloadFile(tempFile, 10L, "report.zip"));

        ResponseEntity<Resource> response = fileController.download(TASK_ID, "bytes=-3");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes 7-9/10");
        assertThat(response.getHeaders().getContentLength()).isEqualTo(3L);

        byte[] body = response.getBody().getInputStream().readAllBytes();
        assertThat(new String(body)).isEqualTo("789");

        Files.deleteIfExists(tempFile);
    }

    @Test
    @DisplayName("GET /files/download 非法 Range 返回 416")
    void given_invalidRangeHeader_when_download_then_return416() throws IOException {
        Path tempFile = Files.createTempFile("jinshu_invalid_", ".xlsx");
        Files.write(tempFile, "0123456789".getBytes());
        tempFile.toFile().deleteOnExit();

        when(fileDownloadService.resolveDownloadFile(TASK_ID))
                .thenReturn(new FileDownloadService.DownloadFile(tempFile, 10L, "report.xlsx"));

        ResponseEntity<Resource> response = fileController.download(TASK_ID, "bytes=20-30");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes */10");

        Files.deleteIfExists(tempFile);
    }

    @Test
    @DisplayName("GET /files/download 多区间 Range 返回 416")
    void given_multiRangeHeader_when_download_then_return416() throws IOException {
        Path tempFile = Files.createTempFile("jinshu_multi_", ".xlsx");
        Files.write(tempFile, "0123456789".getBytes());
        tempFile.toFile().deleteOnExit();

        when(fileDownloadService.resolveDownloadFile(TASK_ID))
                .thenReturn(new FileDownloadService.DownloadFile(tempFile, 10L, "report.xlsx"));

        ResponseEntity<Resource> response = fileController.download(TASK_ID, "bytes=0-1,3-4");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE)).isEqualTo("bytes */10");

        Files.deleteIfExists(tempFile);
    }
}
