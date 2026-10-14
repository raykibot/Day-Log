package com.keng.diary.controller;

import com.keng.diary.model.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiError {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Result<Void>> known(ResponseStatusException error) {
        return reply(error.getStatus(), error.getReason());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> tooLarge() {
        return reply(HttpStatus.PAYLOAD_TOO_LARGE, "上传文件超过大小限制");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> missingParameter(MissingServletRequestParameterException error) {
        return reply(HttpStatus.BAD_REQUEST, "缺少参数：" + error.getParameterName());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Result<Void>> missingPart(MissingServletRequestPartException error) {
        return reply(HttpStatus.BAD_REQUEST, "缺少文件：" + error.getRequestPartName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> wrongType(MethodArgumentTypeMismatchException error) {
        return reply(HttpStatus.BAD_REQUEST, "参数格式错误：" + error.getName());
    }

    private ResponseEntity<Result<Void>> reply(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(Result.error(status.value(), message == null ? status.getReasonPhrase() : message));
    }
}
