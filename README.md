# Enterprise Ticket System

面向企业内部 IT 技术支持场景的工单管理系统。项目当前处于 V1.0 开发阶段，目标是实现“员工提交、主管分派、技术人员处理、员工确认、工单关闭”的完整业务闭环。

## 技术栈

- Java 17
- Spring Boot 4
- MyBatis-Plus
- MySQL 8
- Flyway
- Redis
- Sa-Token
- Springdoc OpenAPI / Swagger UI

## 当前能力

- Controller、Service、Mapper 三层架构；
- 用户原型 CRUD 和分页查询；
- 基于账号密码的 Sa-Token 登录、Redis 会话和数据库 RBAC；
- 统一泛型响应和全局异常处理；
- Controller 日志切面；
- V1.0 数据库、API、状态机和权限设计基线；
- Flyway V1~V3：系统表、工单表和基础数据迁移；
- 工单创建、创建流水、Redis 幂等、分页查询、详情和数据范围校验；
- 工单分派、改派、处理、解决、确认、退回以及乐观并发控制；
- 启用部门/分类查询，以及部门、工单分类分页、新增、修改和启停管理。

用户、角色和权限管理接口及数据库集成测试正在按设计基线逐步实现。

## 开始使用

启动前设置本地数据库密码：

```powershell
$env:DB_PASSWORD = "你的本地数据库密码"
.\mvnw.cmd spring-boot:run
```

当前开发环境默认访问：

```text
http://localhost:8080/dev-api/swagger-ui/index.html
```

首次执行 Flyway 前，建议使用隔离的空数据库验证迁移，不要直接操作唯一的开发或生产数据库。

## 项目文档

按以下顺序阅读：

1. [项目上下文](./docs/PROJECT_CONTEXT.md)
2. [V1.0 需求与概要设计](./docs/V1_REQUIREMENTS_AND_DESIGN.md)
3. [V1.0 数据库设计](./docs/V1_DATABASE_DESIGN.md)
4. [V1.0 API 契约](./docs/V1_API_CONTRACT.md)
5. [本地开发与数据库迁移](./docs/LOCAL_DEVELOPMENT.md)

## 验证

离线编译和打包：

```powershell
.\mvnw.cmd -DskipTests package
```

项目仍处于开发阶段，生产部署配置、管理端接口和完整环境联调尚未完成。
