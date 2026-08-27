package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI 与 OpenAPI 文档配置。
 */
@Configuration
public class SwaggerConfig {

    /**
     * 生成项目的 OpenAPI 文档基础信息。
     */
    @Bean
    public OpenAPI customOpenAPI() {
        Contact contact = new Contact()
                .name("JavaSeAI Design Team");

        Info info = new Info()
                .title("企业内部工单系统 API")
                .description("企业内部工单、用户、角色与权限接口文档")
                .version("1.0.0")
                .contact(contact);

        Components components = new Components()
                .addSecuritySchemes("satoken", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("satoken")
                        .description("登录接口返回的 tokenValue"));

        return new OpenAPI()
                .components(components)
                .info(info);
    }
}
