package com.example.demo.converter;

import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.LoginStatusResponse;
import com.example.demo.entity.User;
import org.springframework.stereotype.Component;

/**
 * 登录领域对象到接口 DTO 的转换器。
 *
 * <p>转换器只负责组装返回对象，不负责查询数据库或创建 Token。</p>
 */
@Component
public class AuthConverter {

    public LoginResponse toLoginResponse(
            User user,
            String tokenName,
            String tokenValue,
            long tokenTimeout) {
        return new LoginResponse(tokenName, tokenValue, user.getId(), tokenTimeout);
    }

    public LoginStatusResponse toLoginStatusResponse(
            boolean loggedIn,
            Object loginId,
            String tokenName,
            Long tokenTimeout) {
        return new LoginStatusResponse(
                loggedIn,
                loggedIn && loginId != null ? String.valueOf(loginId) : null,
                tokenName,
                loggedIn ? tokenTimeout : null
        );
    }
}
