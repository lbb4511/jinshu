package com.jinshu.common.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PageResult 分页结果封装测试")
class PageResultTest {

    @Test
    @DisplayName("构造函数：所有字段正确赋值")
    void given_listAndPageInfo_when_construct_then_fieldsMatch() {
        List<String> items = List.of("a", "b", "c");
        PageResult<String> pr = new PageResult<>(items, 100L, 1, 20);

        assertThat(pr.getList()).containsExactly("a", "b", "c");
        assertThat(pr.getTotal()).isEqualTo(100);
        assertThat(pr.getPageNum()).isEqualTo(1);
        assertThat(pr.getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("of() 静态工厂：字段与构造器一致")
    void given_listAndPageInfo_when_of_then_fieldsMatch() {
        List<Integer> items = List.of(1, 2, 3, 4, 5);
        PageResult<Integer> pr = PageResult.of(items, 50L, 2, 10);

        assertThat(pr.getList()).hasSize(5);
        assertThat(pr.getTotal()).isEqualTo(50);
        assertThat(pr.getPageNum()).isEqualTo(2);
        assertThat(pr.getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("空数据列表：total=0, list 为空")
    void given_emptyList_when_construct_then_emptyOk() {
        List<String> empty = List.of();
        PageResult<String> pr = new PageResult<>(empty, 0L, 1, 20);

        assertThat(pr.getList()).isEmpty();
        assertThat(pr.getTotal()).isZero();
    }

    @Test
    @DisplayName("不同页码：pageNum 正确传递")
    void given_variousPageNum_when_construct_then_pageNumMatches() {
        PageResult<String> pr1 = new PageResult<>(List.of(), 0L, 1, 20);
        PageResult<String> pr5 = new PageResult<>(List.of(), 0L, 5, 20);

        assertThat(pr1.getPageNum()).isEqualTo(1);
        assertThat(pr5.getPageNum()).isEqualTo(5);
    }
}
