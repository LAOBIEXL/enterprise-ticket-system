package com.example.demo.service;

import cn.dev33.satoken.stp.StpUtil;
import com.example.demo.converter.AuthConverter;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.entity.SysUser;
import com.example.demo.exception.InvalidCredentialsException;
import com.example.demo.mapper.SysUserMapper;
import com.example.demo.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTests {

    @Test
    void shouldRejectUnknownUserWithGenericCredentialError() {
        SysUserMapper mapper = mock(SysUserMapper.class);
        when(mapper.selectByUsername("missing")).thenReturn(null);
        AuthServiceImpl service = new AuthServiceImpl(mapper, mock(AuthConverter.class), mock(PasswordEncoder.class));

        assertThatThrownBy(() -> service.login(new LoginRequest("missing", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("用户名或密码错误");
    }

    @Test
    void shouldCreateTokenOnlyAfterBcryptVerification() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setDepartmentId(5L);
        user.setUsername("admin");
        user.setPasswordHash("$2b$10$hash");
        user.setName("系统管理员");
        user.setStatus(1);

        SysUserMapper mapper = mock(SysUserMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        AuthConverter converter = mock(AuthConverter.class);
        LoginResponse expected = new LoginResponse("satoken", "token", null, 2_592_000L);
        when(mapper.selectByUsername("admin")).thenReturn(user);
        when(encoder.matches("correct-password", user.getPasswordHash())).thenReturn(true);
        when(mapper.selectEnabledRoleCodesByUserId(1L)).thenReturn(List.of("SYSTEM_ADMIN"));
        when(mapper.selectEnabledDepartmentNameById(5L)).thenReturn("技术部");
        when(converter.toLoginResponse(user, "技术部", List.of("SYSTEM_ADMIN"), "satoken", "token", 2_592_000L))
                .thenReturn(expected);
        AuthServiceImpl service = new AuthServiceImpl(mapper, converter, encoder);

        try (MockedStatic<StpUtil> stp = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.login(1L)).thenAnswer(invocation -> null);
            stp.when(StpUtil::getTokenName).thenReturn("satoken");
            stp.when(StpUtil::getTokenValue).thenReturn("token");
            stp.when(StpUtil::getTokenTimeout).thenReturn(2_592_000L);

            service.login(new LoginRequest("admin", "correct-password"));
        }

        verify(encoder).matches("correct-password", user.getPasswordHash());
    }
}
