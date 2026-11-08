package com.nanji.interview.ai_interview_system.common;

/**
 * 统一接口返回格式（迁移自 feature/interview-agent 的 ResultVO）。
 * <p>
 * 所有第一周核心接口（简历上传/解析、创建面试、提交回答、结束面试、查询报告）
 * 均使用本结构返回：code=200 表示成功，data 为业务数据。
 */
public class Result<T> {

    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(Constants.SUCCESS_CODE);
        result.setMessage(Constants.SUCCESS_MESSAGE);
        result.setData(data);
        return result;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
