package com.keng.diary.model;

import lombok.Data;

@Data
public class Result<T> {

    private int code;
    private String message;
    private T data;

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(0000, "成功", data);
    }

    public static <T> Result<T> created(T data) {
        return new Result<>(0000, "创建成功", data);
    }

    public static <T>Result<T> error() {
        return new Result<>(1111, "操作失败", null);
    }

    public static <T>Result<T> runTimeException(){
        return new Result<>(1111, "系统运行时异常", null);
    }
}
