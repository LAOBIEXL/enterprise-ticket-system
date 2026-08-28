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

完整只读 Mapper 契约测试还会检查角色权限联表、用户角色联表和工单分页/详情映射：

```powershell
.\mvnw.cmd "-Dtest=DatabaseMapperIntegrationTests" "-Ddatabase.mapper.test.enabled=true" test
```

该测试同样不会写入、更新或删除开发数据。涉及状态更新和事务回滚的写入测试必须使用隔离数据库，不允许连接唯一的开发库。

工单条件更新测试会创建或复用专用的 `enterprise_ticket_it` 数据库，执行 Flyway 后在自动回滚事务中验证“状态 + 版本号”并发保护。该测试不会删除数据库，也不会把测试记录提交到数据库：

```powershell
.\mvnw.cmd "-Dtest=TicketTransactionIntegrationTests" `
  "-Ddatabase.transaction.test.enabled=true" `
  "-Ddatabase.transaction.test.admin-url=jdbc:mysql://127.0.0.1:3306/?serverTimezone=UTC" `
  "-Ddatabase.transaction.test.url=jdbc:mysql://127.0.0.1:3306/enterprise_ticket_it?serverTimezone=UTC" `
  "-Ddatabase.transaction.test.user=root" `
  "-Ddatabase.transaction.test.password=本地密码" test
```

若应用账号没有 `CREATE DATABASE` 权限，可使用隔离 MySQL 容器的 root 账号创建数据库，
同时让测试数据源继续使用最小权限的应用账号：

```powershell
  "-Ddatabase.transaction.test.admin-user=root" `
  "-Ddatabase.transaction.test.admin-password=隔离容器 root 密码"
```

`admin-user`/`admin-password` 只用于 `CREATE DATABASE IF NOT EXISTS`，不会用于业务查询；
省略时默认复用测试数据源账号。

如果隔离容器已经通过 `MYSQL_DATABASE=enterprise_ticket_it` 创建了数据库，可以追加
`-Ddatabase.transaction.test.create-schema=false`，这样测试只使用业务账号连接已有空库，
由 Flyway 执行表结构迁移，不要求业务账号具备建库权限。

## 7. 首个管理员初始化

应用默认不会创建管理员。仅在本地或受控环境首次初始化时，临时设置以下变量：

```powershell
$env:BOOTSTRAP_ADMIN_ENABLED = "true"
$env:BOOTSTRAP_ADMIN_USERNAME = "admin"
$env:BOOTSTRAP_ADMIN_PASSWORD = "至少 12 位的本地强密码"
```

启动成功后，初始化器会创建一个 `SYSTEM_ADMIN` 用户并写入 BCrypt 哈希。重复启动不会重复创建；确认登录成功后应立即清除当前 PowerShell 中的 `BOOTSTRAP_ADMIN_PASSWORD`，并将 `BOOTSTRAP_ADMIN_ENABLED` 恢复为 `false`。

密码不会写入迁移文件、日志、Swagger 或 Git。

## 8. 真实依赖联调

在 MySQL 和 Redis 已启动、应用运行后，可以使用不回显密码和 Token 的冒烟脚本验证登录、
请求头认证、基础数据、管理端查询以及 OpenAPI 安全方案：

```powershell
Get-Content .env.local | ForEach-Object {
  if ($_ -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
    [Environment]::SetEnvironmentVariable($matches[1], $matches[2])
  }
}
$env:SMOKE_BASE_URL = "http://127.0.0.1:8080/dev-api"
.\scripts\smoke-api.ps1
```

脚本只执行登录和 GET 请求，不创建、修改或删除业务数据；登录产生的 Sa-Token 会话会按配置写入 Redis。
可通过 RedisInsight 或 `redis-cli -n 1 --scan --pattern 'satoken*'` 查看会话键（不要复制 Token 值到日志或文档）。
