package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.entity.SysDepartment;
import com.example.demo.entity.SysUser;
import com.example.demo.entity.Ticket;
import com.example.demo.entity.TicketCategory;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 在专用集成测试库验证条件更新；测试数据由 Spring 事务自动回滚。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfSystemProperty(named = "database.transaction.test.enabled", matches = "true")
class TicketTransactionIntegrationTests {

    private static final String URL = System.getProperty("database.transaction.test.url", "");
    private static final String ADMIN_URL = System.getProperty("database.transaction.test.admin-url", "");
    private static final String USER = System.getProperty("database.transaction.test.user", "root");
    private static final String PASSWORD = System.getProperty("database.transaction.test.password", "");
    private static final String ADMIN_USER = System.getProperty("database.transaction.test.admin-user", USER);
    private static final String ADMIN_PASSWORD = System.getProperty(
            "database.transaction.test.admin-password", PASSWORD);
    private static final boolean CREATE_SCHEMA = Boolean.parseBoolean(
            System.getProperty("database.transaction.test.create-schema", "true"));
    private static final String DATABASE_NAME = "enterprise_ticket_it";

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) throws SQLException {
        if (CREATE_SCHEMA) {
            createDatabaseIfNecessary();
        }
        registry.add("spring.datasource.url", () -> URL);
        registry.add("spring.datasource.username", () -> USER);
        registry.add("spring.datasource.password", () -> PASSWORD);
        registry.add("app.bootstrap-admin.enabled", () -> false);
    }

    @Resource
    private SysDepartmentMapper sysDepartmentMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private TicketCategoryMapper ticketCategoryMapper;

    @Resource
    private TicketMapper ticketMapper;

    @Test
    @Transactional
    void shouldRejectStaleTicketStateAndVersionUpdate() {
        SysDepartment department = sysDepartmentMapper.selectOne(
                new LambdaQueryWrapper<SysDepartment>().eq(SysDepartment::getCode, "TECHNOLOGY"));
        TicketCategory category = ticketCategoryMapper.selectOne(
                new LambdaQueryWrapper<TicketCategory>().eq(TicketCategory::getCode, "COMPUTER"));

        SysUser user = new SysUser();
        user.setDepartmentId(department.getId());
        user.setUsername("it_" + UUID.randomUUID().toString().replace("-", ""));
        user.setPasswordHash("integration-test-hash");
        user.setName("集成测试用户");
        user.setStatus(1);
        sysUserMapper.insert(user);

        Ticket ticket = new Ticket();
        ticket.setTicketNo("IT" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        ticket.setTitle("条件更新集成测试");
        ticket.setDescription("该数据会随测试事务回滚");
        ticket.setCategoryId(category.getId());
        ticket.setRequesterId(user.getId());
        ticket.setRequesterDepartmentId(department.getId());
        ticket.setStatus("PENDING");
        ticket.setVersion(0);
        ticketMapper.insert(ticket);

        int firstUpdate = ticketMapper.updateAssignment(
                ticket.getId(), "PENDING", 0, "ASSIGNED", user.getId());
        int staleUpdate = ticketMapper.updateAssignment(
                ticket.getId(), "PENDING", 0, "ASSIGNED", user.getId());

        assertThat(firstUpdate).isEqualTo(1);
        assertThat(staleUpdate).isZero();
        Ticket updated = ticketMapper.selectById(ticket.getId());
        assertThat(updated.getStatus()).isEqualTo("ASSIGNED");
        assertThat(updated.getVersion()).isEqualTo(1);
    }

    private static void createDatabaseIfNecessary() throws SQLException {
        if (URL.isBlank() || ADMIN_URL.isBlank()) {
            throw new IllegalStateException("专用集成测试数据库 URL 未配置");
        }
        try (Connection connection = DriverManager.getConnection(ADMIN_URL, ADMIN_USER, ADMIN_PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DATABASE_NAME
                    + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
    }
}
