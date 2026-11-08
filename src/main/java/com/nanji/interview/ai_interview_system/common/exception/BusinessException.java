package com.nanji.interview.ai_interview_system.common.exception;

/**
 * 业务异常：由 GlobalExceptionHandler 统一转换为 Result（code=400）。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
