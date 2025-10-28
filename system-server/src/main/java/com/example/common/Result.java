package com.example.common;

import lombok.Data;

@Data
public class Result<T> {
    private Integer status;
    private String message;
    private T data;

    private Result(Integer status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    public static Result<Void> success(String message) {
        return new Result<>(200, message, null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, null, data);
    }

    public static Result<Void> success() {
        return new Result<>(200, null, null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    public static Result<Void> error() {
        return new Result<>(500, null, null);
    }
}
