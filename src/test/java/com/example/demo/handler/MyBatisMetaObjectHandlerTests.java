package com.example.demo.handler;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.demo.entity.User;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MyBatisMetaObjectHandlerTests {

    private final MyBatisMetaObjectHandler handler = new MyBatisMetaObjectHandler();

    @BeforeAll
    static void initializeMyBatisTableMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        TableInfoHelper.initTableInfo(assistant, User.class);
    }

    @Test
    void shouldFillCreateAndUpdateTimeOnInsert() {
        User user = new User();

        handler.insertFill(SystemMetaObject.forObject(user));

        assertNotNull(user.getCreateTime());
        assertNotNull(user.getUpdateTime());
        assertEquals(user.getCreateTime(), user.getUpdateTime());
    }

    @Test
    void shouldOnlyRefreshUpdateTimeOnUpdate() {
        User user = new User();
        LocalDateTime createTime = LocalDateTime.of(2026, 1, 1, 0, 0);
        user.setCreateTime(createTime);

        handler.updateFill(SystemMetaObject.forObject(user));

        assertEquals(createTime, user.getCreateTime());
        assertNotNull(user.getUpdateTime());
    }
}
