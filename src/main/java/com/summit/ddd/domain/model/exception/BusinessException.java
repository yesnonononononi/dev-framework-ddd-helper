package com.summit.ddd.domain.model.exception;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
    public BusinessException() {
        super("服务错误");
    }
}
