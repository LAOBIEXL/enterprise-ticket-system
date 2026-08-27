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
$env:DB_URL = "jdbc:mysql://127.0.0.1:3306/demo?characterEncoding=UTF-8&serverTimezone=UTC"
$env:DB_USERNAME = "root"
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

现有开发库中已经存在原型 `user` 表，因此配置使用基线版本 `0`：

- Flyway 创建 `flyway_schema_history`；
- 保留旧 `user` 表；
- 从 V1 开始创建新表；
- 不执行删除和数据清空。

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
