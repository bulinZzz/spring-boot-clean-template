package com.xingyun.template.module.example.domain.exception;

/**
 * 领域业务异常：重命名违反聚合根业务规则时抛出。
 */
public class ExampleRenameException extends RuntimeException {

    public ExampleRenameException(String message) {
        super(message);
    }
}
