package com.example.demo.config;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoForRedisTemplate;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.flyway.enabled=false")
class SaTokenConfigTests {

    @Resource
    private SaTokenConfig saTokenConfig;

    @Resource
    private SaTokenDao saTokenDao;

    @Test
    void shouldBindSaTokenProperties() {
        assertThat(saTokenConfig.getTokenName()).isEqualTo("satoken");
        assertThat(saTokenConfig.getTimeout()).isEqualTo(2_592_000L);
        assertThat(saTokenConfig.getActiveTimeout()).isEqualTo(-1L);
        assertThat(saTokenConfig.getIsConcurrent()).isTrue();
        assertThat(saTokenConfig.getIsShare()).isFalse();
        assertThat(saTokenConfig.getTokenStyle()).isEqualTo("uuid");
        assertThat(saTokenConfig.getIsReadCookie()).isFalse();
        assertThat(saTokenConfig.getIsWriteHeader()).isTrue();
        assertThat(saTokenConfig.getIsLog()).isTrue();
        assertThat(saTokenDao).isInstanceOf(SaTokenDaoForRedisTemplate.class);
    }
}
