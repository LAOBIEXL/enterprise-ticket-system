package com.example.demo.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结果类
 */
@Data
@Schema(name = "Result", description = "统一响应体")
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "业务状态码", example = "200")
    private int code;

    @Schema(description = "提示信息", example = "操作成功")
    private String msg;

    @Schema(description = "业务数据")
    private T data;

    /**
     * 无参构造
     */
    public Result() {
    }

    /**
     * 全参构造
     */
    public Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 直接返回成功结果
     */
    public static <T> Result<T> success(T data) {
        return success(200, "操作成功", data);
    }

    /**
     * 成功，无返回数据
     */
    public static Result<Void> success() {
        return success(200, "操作成功", null);
    }

    /**
     * 自定义返回成功结果
     */
    public static <T> Result<T> success(int code, String msg, T data) {
        return new Result<>(code, msg, data);
    }

    /**
     * 返回失败结果
     */
    public static Result<Void> fail(String msg) {
        return fail(500, msg, null);
    }

    /**
     * 自定义返回失败结果
     */
    public static Result<Void> fail(int code, String msg) {
        return fail(code, msg, null);
    }

    /**
     * 自定义返回失败结果
     */
    public static <T> Result<T> fail(int code, String msg, T data) {
        return new Result<>(code, msg, data);
    }
}
