package com.keng.diary.model;

import lombok.Data;

@Data
public class ResponseEntity<T> {

    private int code;
    private String message;
    private T data;

    public ResponseEntity(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ResponseEntity<T> success(T data) {
        return new ResponseEntity<>(200, "成功", data);
    }

    public static <T> ResponseEntity<T> created(T data) {
        return new ResponseEntity<>(201, "创建成功", data);
    }

    public static ResponseEntity<Void> error(int code, String message) {
        return new ResponseEntity<>(code, message, null);
    }
}
