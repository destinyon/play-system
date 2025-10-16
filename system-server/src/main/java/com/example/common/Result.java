package com.example.common;

import lombok.Data;

@Data
public class Result {
    private Integer status;
    private String message;
    private Object data;

    private Result(Integer status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }
    public static Result success(String message, Object data) {
        return new Result(200, message, data);
    }

    public static Result success(String message) {
        return new Result(200, message, null);
    }
    public static Result error(String message) {
        return new Result(500, message, null);
    }
    public static Result success(Object data) {
        return new Result(200, null, data);
    }
    public static Result success() {
        return new Result(200, null, null);
    }
    public static Result error() {
        return new Result(500, null, null);
    }
}
