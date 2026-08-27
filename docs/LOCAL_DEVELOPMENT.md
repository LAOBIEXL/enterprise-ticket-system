# 本地开发与数据库迁移

## 1. 必需环境

- JDK 17；
- Maven Wrapper（项目已包含）；
- MySQL 8.x；
- Redis；
- 可选：Docker Desktop，用于隔离数据库验证。

## 2. 数据库环境变量

开发配置不再保存明文数据库密码。启动应用前至少设置：

```powershell
$env:DB_PASSWORD = "你的本地数据库密码"
```

该写法只在当前 PowerShell 窗口有效。关闭窗口后自动失效，不会写入项目文件。

以下变量可选，未设置时使用开发默认值：

```powershell
$env:DB_URL = "jdbc:mysql://127.0.0.1:3306/enterprise_ticket_dev?characterEncoding=UTF-8&serverTimezone=UTC"
$env:DB_USERNAME = "ticket_app"
```

在 IntelliJ IDEA 中运行时，可以打开：

```text
Run/Debug Configurations
→ DemoApplication
→ Environment variables
→ 添加 DB_PASSWORD
```

不要把真实密码写入 `application-*.yaml`、Markdown、提交信息或截图。项目已经忽略 `.env` 文件，但 Spring Boot 默认不会自动加载 `.env`。

## 3. Flyway 行为

应用启动时，Flyway 会在业务代码访问数据库前执行：

```text
V1 创建系统和 RBAC 表
→ V2 创建工单表
→ V3 初始化部门、分类、角色和权限
```

对于曾经包含原型 `user` 表且尚未纳入 Flyway 的旧开发库，配置使用基线版本 `0`：

- Flyway 创建 `flyway_schema_history`；
- 保留旧 `user` 表；
- 从 V1 开始创建新表；
- 不执行删除和数据清空。

当前标准开发库为 `enterprise_ticket_dev`，已经执行 V1~V3。新建空库时会直接从 V1 开始迁移，不会产生基线记录。

`clean-disabled: true` 禁止通过 Flyway Clean 清空数据库。

## 4. 推荐验证方式

首次迁移不要直接在唯一的开发库上验证。推荐：

1. 启动 Docker Desktop；
2. 创建一个临时 MySQL 8 容器和空数据库；
3. 将 `DB_URL` 指向临时数据库；
4. 启动应用并观察 V1~V3 是否全部成功；
5. 检查 9 张业务表和 `flyway_schema_history`；
6. 验证角色、权限和分类初始数据；
7. 隔离验证通过后，再决定何时迁移现有开发库。

## 5. 常用验证命令

只编译和打包，不启动应用、不连接数据库：

```powershell
.\mvnw.cmd -DskipTests package
```

运行不依赖真实数据库的单元测试：

```powershell
.\mvnw.cmd "-Dtest=ControllerLogAspectTests,RedisConfigTests,AuthControllerTests,UserControllerPermissionTests,MyBatisMetaObjectHandlerTests,StpInterfaceImplTests,RedisUtilsTests" test
```

## 6. 自动化迁移验收

迁移集成测试位于：

```text
src/test/java/com/example/demo/migration/FlywayMigrationTests.java
```

测试在未提供 `migration.test.url` 时自动跳过，不会误连本地开发数据库。只有显式传入隔离数据库地址后才会执行迁移。

测试会核对：

- V1~V3 共 3 个迁移全部成功；
- 创建 9 张目标业务表；
- 创建 12 个外键和 8 个检查约束；
- 初始化 5 个部门、5 个分类、4 个角色、15 个权限和 25 条角色权限关系；
- 第二次执行迁移时新增迁移数为 0。

2026-08-27 已使用官方 MySQL 8.0 Docker 镜像完成隔离验证。MySQL 8.4 同样执行成功，但当前 Flyway 会提示该数据库版本高于其已验证版本，因此 V1.0 的正式兼容基线采用 MySQL 8.0。

正式用户 Mapper 的只读集成测试默认关闭。需要验证 Mapper XML 与本地开发库时，在已经设置数据库环境变量的 PowerShell 中执行：

```powershell
.\mvnw.cmd "-Dtest=SysUserMapperIntegrationTests" "-Dsys-user.mapper.test.enabled=true" test
```

该测试只查询一个明确不存在的用户，不插入、更新或删除开发数据。
