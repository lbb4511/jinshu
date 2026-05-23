package com.jinshu.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HashUtils 工具类测试")
class HashUtilsTest {

    private static final String EXPECTED_HELLO_SHA256 =
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

    @Test
    @DisplayName("SHA-256：对已知字符串计算，结果与预期一致")
    void given_knownString_when_sha256_then_matchExpected() {
        String hash = HashUtils.sha256("hello");
        assertThat(hash).isEqualTo(EXPECTED_HELLO_SHA256);
    }

    @Test
    @DisplayName("SHA-256：空字符串可正常计算")
    void given_emptyString_when_sha256_then_returnHash() {
        String hash = HashUtils.sha256("");
        assertThat(hash).isNotBlank();
        assertThat(hash).hasSize(64);
    }

    @Test
    @DisplayName("SHA-256：重复输入得到相同哈希值（幂等性）")
    void given_sameInputTwice_when_sha256_then_hashesEqual() {
        String hash1 = HashUtils.sha256("test-data");
        String hash2 = HashUtils.sha256("test-data");
        assertThat(hash1).isEqualTo(hash2);
    }

    @ParameterizedTest
    @CsvSource({
            "a, ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb",
            "abc, ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            "hello, 2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
    })
    @DisplayName("SHA-256：参数化测试多个已知值")
    void given_variousInputs_when_sha256_then_matchExpected(String input, String expectedHash) {
        assertThat(HashUtils.sha256(input)).isEqualTo(expectedHash);
    }

    @Test
    @DisplayName("SHA-256：输出始终为 64 位十六进制字符")
    void given_anyInput_when_sha256_then_outputLength64() {
        String hash = HashUtils.sha256("some-arbitrary-input-with-mixed-chars!@#$%^&*()");
        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("SHA-256：长字符串也能正常计算")
    void given_longInput_when_sha256_then_notEmpty() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100000; i++) {
            sb.append("a");
        }
        String hash = HashUtils.sha256(sb.toString());
        assertThat(hash).hasSize(64);
    }

    @Test
    @DisplayName("SHA-256：不同输入产生不同哈希")
    void given_differentInputs_when_sha256_then_differentHashes() {
        String hash1 = HashUtils.sha256("input1");
        String hash2 = HashUtils.sha256("input2");
        assertThat(hash1).isNotEqualTo(hash2);
    }
}
