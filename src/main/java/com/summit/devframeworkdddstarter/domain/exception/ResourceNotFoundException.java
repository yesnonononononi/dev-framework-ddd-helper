package com.summit.devframeworkdddstarter.domain.exception;

/**
 * 领域层（Domain）资源未找到异常
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException() {
        super("未找到相关数据");
    }
}
