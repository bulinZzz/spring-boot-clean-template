package com.xingyun.template.shared.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常 → HTTP 翻译器：全项目唯一的 {@code @RestControllerAdvice}，业务代码不捕获
 * 技术故障，异常传播到 HTTP 边界后在此统一翻译为响应。
 *
 * <p>映射判据：{@code IllegalStateException} 是本项目"聚合不变量被违反"的统一词汇，
 * 属调用方可纠正的业务拒绝，翻译为 409 Conflict；异常消息面向开发者日志而非对外契约，
 * 故响应体为空，状态码即对外语义。新异常类型的映射随首个真实需要在此增补，
 * 不设无消费方的处理器。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Void> onIllegalState(IllegalStateException exception) {
        log.warn("聚合不变量违规：{}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
}
