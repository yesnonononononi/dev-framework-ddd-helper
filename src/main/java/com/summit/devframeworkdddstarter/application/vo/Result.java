package com.summit.devframeworkdddstarter.application.vo;




import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;


@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode
public class Result<T> implements Serializable {

    private int code;


    private T data;

    private Instant timestamp;

    private String errMsg;

    private String signStr;

    public Result(int code, T data, Instant timestamp) {
        this.code = code;
        this.data = data;
        this.timestamp = timestamp;
    }


    public static <T> Result<T> success(Integer code, T data) {
        code = Objects.requireNonNullElse(code, 1);
        return new Result<>(code, data, Instant.now());
    }

    public static <T> Result<T> success(Integer code) {
        return success(code, null);
    }

    public static <T> Result<T> success(T data) {
        return success(1, data);
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static  Result<String> error(Integer code, String errMsg) {
        code = Objects.requireNonNullElse(code, 0);
        return new Result<>(code, errMsg, Instant.now());
    }

    public static  Result<String> error(String errMsg) {
        return error(0, errMsg);
    }

    public static Result<String> error(Integer code) {
        return error(code, null);
    }

    public static  Result<String> error() {
        return error(0);
    }


}
