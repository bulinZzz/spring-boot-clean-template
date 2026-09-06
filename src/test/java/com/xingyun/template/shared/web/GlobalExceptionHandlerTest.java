package com.xingyun.template.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.xingyun.template.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unexpected_exception_should_return_500_without_internal_detail() {
        ProblemDetail problemDetail =
                handler.onUnexpectedException(
                        new IllegalStateException("database password=secret")
                );

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Internal Server Error");
        assertThat(problemDetail.getDetail()).isEqualTo("服务器内部错误");
    }

    /**
     * 回归：标准 JDK 异常不代表业务规则拒绝，只有 {@link BusinessException} 才进入业务异常处理路径。
     */
    @Test
    void illegal_state_exception_should_not_be_treated_as_business_exception() {
        ProblemDetail problemDetail =
                handler.onUnexpectedException(
                        new IllegalStateException("some internal state problem")
                );

        assertThat(problemDetail.getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());

        assertThat(problemDetail.getDetail())
                .isEqualTo("服务器内部错误");
    }

    @Test
    void business_exception_should_return_409_problem_detail() {
        ProblemDetail problemDetail =
                handler.onBusinessException(
                        new BusinessException("新名称与当前名称相同")
                );

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Business Rule Violation");
        assertThat(problemDetail.getDetail()).isEqualTo("新名称与当前名称相同");
    }
}
