package com.example.demo.service;

import com.example.demo.config.BootstrapAdminProperties;
import com.example.demo.entity.SysUser;
import com.example.demo.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 在明确启用时创建首个系统管理员，适合本地或受控部署初始化。
 */
@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);
    private static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";

    private final BootstrapAdminProperties properties;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

    public BootstrapAdminInitializer(
            BootstrapAdminProperties properties,
            SysUserMapper sysUserMapper,
            PasswordEncoder passwordEncoder,
            TransactionTemplate transactionTemplate) {
        this.properties = properties;
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        validateProperties();
        transactionTemplate.executeWithoutResult(status -> initialize());
    }

    private void initialize() {
        SysUser existingUser = sysUserMapper.selectByUsername(properties.getUsername());
        if (existingUser != null) {
            if (sysUserMapper.selectEnabledRoleCodesByUserId(existingUser.getId()).contains(SYSTEM_ADMIN)) {
                log.info("Bootstrap admin already exists; initialization skipped");
                return;
            }
            throw new IllegalStateException("Bootstrap username already exists but is not a system administrator");
        }

        if (sysUserMapper.countUsersByRoleCode(SYSTEM_ADMIN) > 0) {
            log.info("A system administrator already exists; bootstrap initialization skipped");
            return;
        }

        Long departmentId = sysUserMapper.selectEnabledDepartmentIdByCode(properties.getDepartmentCode());
        Long roleId = sysUserMapper.selectEnabledRoleIdByCode(SYSTEM_ADMIN);
        if (departmentId == null || roleId == null) {
            throw new IllegalStateException("Bootstrap department or SYSTEM_ADMIN role is unavailable");
        }

        SysUser user = new SysUser();
        user.setDepartmentId(departmentId);
        user.setUsername(properties.getUsername());
        user.setPasswordHash(passwordEncoder.encode(properties.getPassword()));
        user.setName(properties.getName());
        user.setStatus(1);
        sysUserMapper.insert(user);
        if (sysUserMapper.insertUserRole(user.getId(), roleId) != 1) {
            throw new IllegalStateException("Bootstrap administrator role assignment failed");
        }
        log.info("Bootstrap system administrator created: username={}", properties.getUsername());
    }

    private void validateProperties() {
        if (properties.getUsername() == null || properties.getUsername().isBlank()
                || properties.getUsername().length() > 64) {
            throw new IllegalStateException("Bootstrap admin username must contain 1 to 64 characters");
        }
        if (properties.getPassword() == null || properties.getPassword().length() < 12
                || properties.getPassword().length() > 64) {
            throw new IllegalStateException("Bootstrap admin password must contain 12 to 64 characters");
        }
        if (properties.getName() == null || properties.getName().isBlank()
                || properties.getDepartmentCode() == null || properties.getDepartmentCode().isBlank()) {
            throw new IllegalStateException("Bootstrap admin name and department code are required");
        }
    }
}
