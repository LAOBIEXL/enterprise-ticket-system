package com.example.demo.converter;

import com.example.demo.dto.CurrentUserResponse;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.LoginStatusResponse;
import com.example.demo.dto.LoginUserResponse;
import com.example.demo.entity.SysUser;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 登录领域对象到接口 DTO 的转换器。
 *
 * <p>转换器只负责组装返回对象，不负责查询数据库或创建 Token。</p>
 */
@Component
public class AuthConverter {

    public LoginResponse toLoginResponse(
            SysUser user,
            String departmentName,
            List<String> roles,
            String tokenName,
            String tokenValue,
            long tokenTimeout) {
        LoginUserResponse loginUser = new LoginUserResponse(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getName(),
                departmentName,
                List.copyOf(roles)
        );
        return new LoginResponse(tokenName, tokenValue, loginUser, tokenTimeout);
    }

    public CurrentUserResponse toCurrentUserResponse(
            SysUser user,
            String departmentName,
            List<String> roles,
            List<String> permissions) {
        return new CurrentUserResponse(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getName(),
                departmentName,
                List.copyOf(roles),
                List.copyOf(permissions)
        );
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
