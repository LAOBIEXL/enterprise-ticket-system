package com.example.demo.mapper;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 对真实 MySQL 映射执行只读验证，默认不运行，避免测试误连开发数据库。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfSystemProperty(named = "sys-user.mapper.test.enabled", matches = "true")
class SysUserMapperIntegrationTests {

    private static final long NON_EXISTENT_USER_ID = -1L;
    private static final String NON_EXISTENT_USERNAME = "__mapper_contract_missing_user__";

    @Resource
    private SysUserMapper sysUserMapper;

    @Test
    void shouldLoadMapperXmlAndExecuteUserAndRbacQueries() {
        assertThat(sysUserMapper.selectByUsername(NON_EXISTENT_USERNAME)).isNull();
        assertThat(sysUserMapper.selectEnabledRoleCodesByUserId(NON_EXISTENT_USER_ID)).isEmpty();
        assertThat(sysUserMapper.selectEnabledPermissionCodesByUserId(NON_EXISTENT_USER_ID)).isEmpty();
    }
}
