package com.example.demo.exception;

/**
 * 账号不存在、账号停用或密码错误统一使用该异常，避免泄露账号状态。
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("用户名或密码错误");
    }
}
