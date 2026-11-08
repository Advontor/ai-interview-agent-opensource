package com.nanji.interview.ai_interview_system.common.exception;

import com.nanji.interview.ai_interview_system.common.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理：所有异常统一转换为 Result 结构返回，
 * 并设置真实的 HTTP 状态码（避免二进制下载类接口把错误 JSON 当成正常文件下载）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 参数校验失败（@Valid） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex,
                                                              HttpServletResponse response) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        return Result.error(HttpStatus.BAD_REQUEST.value(), message);
    }

    /** 业务异常（如会话不存在、面试已结束等） */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException ex, HttpServletResponse response) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        return Result.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
    }

    /** 兜底异常（如 DeepSeek 调用失败） */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex, HttpServletResponse response) {
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage());
    }
}
