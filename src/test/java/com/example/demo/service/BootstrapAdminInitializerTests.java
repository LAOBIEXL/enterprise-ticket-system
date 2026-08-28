package com.example.demo.service;

import com.example.demo.config.BootstrapAdminProperties;
import com.example.demo.entity.SysUser;
import com.example.demo.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BootstrapAdminInitializerTests {

    @Test
    void shouldNotTouchDatabaseWhenBootstrapIsDisabled() {
        BootstrapAdminProperties properties = new BootstrapAdminProperties();
        SysUserMapper mapper = mock(SysUserMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        BootstrapAdminInitializer initializer = new BootstrapAdminInitializer(properties, mapper, encoder, transactionTemplate);

        initializer.run(new DefaultApplicationArguments());

        verify(transactionTemplate, never()).executeWithoutResult(any());
        verify(mapper, never()).selectByUsername(any());
    }

    @Test
    void shouldCreateAdministratorInsideTransactionWhenEnabled() {
        BootstrapAdminProperties properties = new BootstrapAdminProperties();
        properties.setEnabled(true);
        properties.setUsername("admin");
        properties.setPassword("a-strong-local-password");
        SysUserMapper mapper = mock(SysUserMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        when(mapper.selectByUsername("admin")).thenReturn(null);
        when(mapper.countUsersByRoleCode("SYSTEM_ADMIN")).thenReturn(0L);
        when(mapper.selectEnabledDepartmentIdByCode("TECHNOLOGY")).thenReturn(5L);
        when(mapper.selectEnabledRoleIdByCode("SYSTEM_ADMIN")).thenReturn(4L);
        when(encoder.encode("a-strong-local-password")).thenReturn("$2b$10$hash");
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<org.springframework.transaction.TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        doAnswer(invocation -> {
            invocation.<SysUser>getArgument(0).setId(1L);
            return 1;
        }).when(mapper).insert(any(SysUser.class));
        when(mapper.insertUserRole(1L, 4L)).thenReturn(1);

        new BootstrapAdminInitializer(properties, mapper, encoder, transactionTemplate)
                .run(new DefaultApplicationArguments());

        verify(mapper).insertUserRole(1L, 4L);
        verify(encoder).encode("a-strong-local-password");
    }

    @Test
    void shouldRejectShortBootstrapPassword() {
        BootstrapAdminProperties properties = new BootstrapAdminProperties();
        properties.setEnabled(true);
        properties.setUsername("admin");
        properties.setPassword("short");
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

        assertThatThrownBy(() -> new BootstrapAdminInitializer(
                properties, mock(SysUserMapper.class), mock(PasswordEncoder.class), transactionTemplate
        ).run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("12 to 64");
    }
}
