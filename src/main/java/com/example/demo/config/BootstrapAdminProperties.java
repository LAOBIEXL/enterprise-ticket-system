package com.example.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 首个管理员初始化配置。默认关闭，密码只从环境变量注入。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.bootstrap-admin")
public class BootstrapAdminProperties {

    private boolean enabled;
    private String username;
    private String password;
    private String name = "系统管理员";
    private String departmentCode = "TECHNOLOGY";
}
