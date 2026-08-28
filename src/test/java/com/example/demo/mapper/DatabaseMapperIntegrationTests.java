package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.entity.SysPermission;
import com.example.demo.entity.SysRole;
import com.example.demo.mapper.model.TicketQueryCriteria;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 对开发 MySQL 执行只读 Mapper 契约验证；必须显式启用，不写入或删除任何业务数据。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfSystemProperty(named = "database.mapper.test.enabled", matches = "true")
class DatabaseMapperIntegrationTests {

    private static final long NON_EXISTENT_ID = -1L;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysPermissionMapper sysPermissionMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private TicketMapper ticketMapper;

    @Test
    void shouldLoadRolePermissionAndUserRelationMappings() {
        SysRole adminRole = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, "SYSTEM_ADMIN"));

        assertThat(adminRole).isNotNull();
        assertThat(sysRoleMapper.selectPermissionsByRoleId(adminRole.getId()))
                .extracting("code")
                .contains("user:manage", "role:manage", "permission:manage");
        assertThat(sysPermissionMapper.selectCount(new LambdaQueryWrapper<SysPermission>()))
                .isGreaterThanOrEqualTo(15);
        assertThat(sysUserMapper.selectRolesByUserId(NON_EXISTENT_ID)).isEmpty();
        assertThat(sysRoleMapper.countUserRole(NON_EXISTENT_ID, adminRole.getId())).isZero();
    }

    @Test
    void shouldLoadTicketQueryMappingsWithoutMutatingData() {
        TicketQueryCriteria criteria = new TicketQueryCriteria(null, null, null, null, null);

        long total = ticketMapper.countByCriteria(criteria);

        assertThat(total).isGreaterThanOrEqualTo(0);
        assertThat(ticketMapper.selectPageByCriteria(criteria, 0, 10)).hasSizeLessThanOrEqualTo(10);
        assertThat(ticketMapper.selectDetailById(NON_EXISTENT_ID)).isNull();
        assertThat(ticketMapper.selectRecordsByTicketId(NON_EXISTENT_ID)).isEmpty();
    }
}
