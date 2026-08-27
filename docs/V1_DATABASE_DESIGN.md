# 企业内部工单系统 V1.0 数据库设计

> 状态：已确认内部开发基线
> 依据：[V1 需求与概要设计](./V1_REQUIREMENTS_AND_DESIGN.md)
> 说明：本文定义目标数据模型，不代表数据库已经执行迁移。

## 1. 设计目标

V1.0 数据库需要支持以下核心能力：

- 公司部门、用户、角色和权限管理；
- 员工创建、查看并确认自己的工单；
- 技术支持主管分派和改派工单；
- 技术人员接单、记录处理过程并提交解决结果；
- 完整保存工单状态变化和操作记录；
- 支持分页查询、权限校验、审计和并发控制；
- 为后续附件、通知、超时提醒和统计分析保留扩展空间。

## 2. 全局约定

### 2.1 数据库与命名

- 数据库：MySQL 8.x；
- 字符集：`utf8mb4`；
- 表名、字段名使用小写下划线；
- 主键统一为 `BIGINT`，Java 对应 `Long`；
- 主数据的启停状态使用 `TINYINT`：`1` 表示启用，`0` 表示停用；
- 工单状态和操作类型使用可读的 `VARCHAR` 枚举值；
- 时间在服务端统一生成，数据库保存统一时区下的时间；
- 业务表不保存明文密码、Token 或其他密钥。

### 2.2 审计字段

需要修改的主数据表默认包含：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `create_by` | BIGINT | 创建人 ID，可为空 |
| `create_time` | DATETIME | 创建时间，不为空 |
| `update_by` | BIGINT | 最后修改人 ID，可为空 |
| `update_time` | DATETIME | 最后修改时间，不为空 |

`ticket_record` 是只追加的审计流水，只保留 `create_time`，不提供更新和删除能力。

### 2.3 删除与外键策略

- 部门、用户、角色、权限和工单分类采用“停用”，不物理删除；
- 工单和工单记录不提供物理删除；
- 稳定业务关系建立外键，并使用 `ON DELETE RESTRICT`；
- `create_by`、`update_by` 等审计字段不建立外键，避免初始化数据、历史账号和数据修复相互阻塞；
- 应用层仍然必须校验对象是否存在和是否可用，不能只依赖外键。

## 3. 枚举定义

### 3.1 工单状态 `ticket.status`

| 值 | 中文含义 |
| --- | --- |
| `PENDING` | 待分派 |
| `ASSIGNED` | 已分派 |
| `PROCESSING` | 处理中 |
| `WAIT_CONFIRM` | 待员工确认 |
| `CLOSED` | 已关闭 |

### 3.2 工单操作 `ticket_record.action`

| 值 | 中文含义 |
| --- | --- |
| `CREATE` | 创建工单 |
| `ASSIGN` | 首次分派 |
| `REASSIGN` | 改派 |
| `START` | 开始处理 |
| `ADD_RECORD` | 添加处理记录 |
| `RESOLVE` | 提交解决结果 |
| `CONFIRM` | 员工确认解决并关闭 |
| `RETURN` | 员工退回继续处理 |

## 4. 表结构设计

### 4.1 `sys_department` 部门表

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 部门 ID |
| `code` | VARCHAR(50) | NOT NULL, UNIQUE | 部门编码 |
| `name` | VARCHAR(100) | NOT NULL | 部门名称 |
| `status` | TINYINT | NOT NULL, DEFAULT 1 | 是否启用 |
| `sort_order` | INT | NOT NULL, DEFAULT 0 | 显示顺序 |
| `create_by` | BIGINT | NULL | 创建人 |
| `create_time` | DATETIME | NOT NULL | 创建时间 |
| `update_by` | BIGINT | NULL | 修改人 |
| `update_time` | DATETIME | NOT NULL | 修改时间 |

索引：

- 唯一索引 `uk_department_code(code)`；
- 普通索引 `idx_department_status_sort(status, sort_order)`。

### 4.2 `sys_user` 用户表

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 用户 ID |
| `department_id` | BIGINT | NOT NULL, FK | 所属部门 |
| `username` | VARCHAR(64) | NOT NULL, UNIQUE | 登录账号 |
| `password_hash` | VARCHAR(255) | NOT NULL | 密码哈希，禁止保存明文 |
| `name` | VARCHAR(100) | NOT NULL | 用户姓名 |
| `email` | VARCHAR(128) | NULL, UNIQUE | 邮箱 |
| `mobile` | VARCHAR(32) | NULL | 手机号 |
| `status` | TINYINT | NOT NULL, DEFAULT 1 | 是否启用 |
| `create_by` | BIGINT | NULL | 创建人 |
| `create_time` | DATETIME | NOT NULL | 创建时间 |
| `update_by` | BIGINT | NULL | 修改人 |
| `update_time` | DATETIME | NOT NULL | 修改时间 |

外键与索引：

- `department_id -> sys_department.id`，删除受限；
- 唯一索引 `uk_user_username(username)`；
- 唯一索引 `uk_user_email(email)`，允许多个 `NULL`；
- 普通索引 `idx_user_department_status(department_id, status)`。

说明：现有原型 `user.age` 不属于工单系统核心业务，目标模型不再保留；登录也将从“按用户 ID 登录”的演示方式改为账号和密码登录。

### 4.3 `sys_role` 角色表

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 角色 ID |
| `code` | VARCHAR(64) | NOT NULL, UNIQUE | 角色编码 |
| `name` | VARCHAR(100) | NOT NULL | 角色名称 |
| `description` | VARCHAR(255) | NULL | 角色说明 |
| `status` | TINYINT | NOT NULL, DEFAULT 1 | 是否启用 |
| `create_by` | BIGINT | NULL | 创建人 |
| `create_time` | DATETIME | NOT NULL | 创建时间 |
| `update_by` | BIGINT | NULL | 修改人 |
| `update_time` | DATETIME | NOT NULL | 修改时间 |

索引：唯一索引 `uk_role_code(code)`。

V1.0 预置角色编码：`EMPLOYEE`、`TECHNICIAN`、`SUPPORT_MANAGER`、`SYSTEM_ADMIN`。

### 4.4 `sys_permission` 权限表

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 权限 ID |
| `code` | VARCHAR(100) | NOT NULL, UNIQUE | Sa-Token 权限编码 |
| `name` | VARCHAR(100) | NOT NULL | 权限名称 |
| `description` | VARCHAR(255) | NULL | 权限说明 |
| `status` | TINYINT | NOT NULL, DEFAULT 1 | 是否启用 |
| `create_by` | BIGINT | NULL | 创建人 |
| `create_time` | DATETIME | NOT NULL | 创建时间 |
| `update_by` | BIGINT | NULL | 修改人 |
| `update_time` | DATETIME | NOT NULL | 修改时间 |

索引：唯一索引 `uk_permission_code(code)`。

权限编码以业务能力命名，例如 `ticket:create`、`ticket:assign`、`ticket:start`、`ticket:read:all`、`user:manage`。

### 4.5 `sys_user_role` 用户角色关联表

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 关联 ID |
| `user_id` | BIGINT | NOT NULL, FK | 用户 ID |
| `role_id` | BIGINT | NOT NULL, FK | 角色 ID |
| `create_by` | BIGINT | NULL | 分配人 |
| `create_time` | DATETIME | NOT NULL | 分配时间 |

外键与索引：

- `user_id -> sys_user.id`；
- `role_id -> sys_role.id`；
- 唯一索引 `uk_user_role(user_id, role_id)`，防止重复授权；
- 普通索引 `idx_user_role_role(role_id, user_id)`，支持按角色反查用户。

### 4.6 `sys_role_permission` 角色权限关联表

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 关联 ID |
| `role_id` | BIGINT | NOT NULL, FK | 角色 ID |
| `permission_id` | BIGINT | NOT NULL, FK | 权限 ID |
| `create_by` | BIGINT | NULL | 分配人 |
| `create_time` | DATETIME | NOT NULL | 分配时间 |

外键与索引：

- `role_id -> sys_role.id`；
- `permission_id -> sys_permission.id`；
- 唯一索引 `uk_role_permission(role_id, permission_id)`；
- 普通索引 `idx_role_permission_permission(permission_id, role_id)`。

### 4.7 `ticket_category` 工单分类表

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 分类 ID |
| `code` | VARCHAR(50) | NOT NULL, UNIQUE | 分类编码 |
| `name` | VARCHAR(100) | NOT NULL | 分类名称 |
| `description` | VARCHAR(255) | NULL | 分类说明 |
| `status` | TINYINT | NOT NULL, DEFAULT 1 | 是否可选 |
| `sort_order` | INT | NOT NULL, DEFAULT 0 | 显示顺序 |
| `create_by` | BIGINT | NULL | 创建人 |
| `create_time` | DATETIME | NOT NULL | 创建时间 |
| `update_by` | BIGINT | NULL | 修改人 |
| `update_time` | DATETIME | NOT NULL | 修改时间 |

索引：

- 唯一索引 `uk_ticket_category_code(code)`；
- 普通索引 `idx_ticket_category_status_sort(status, sort_order)`。

### 4.8 `ticket` 工单主表

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 工单 ID |
| `ticket_no` | VARCHAR(32) | NOT NULL, UNIQUE | 对外展示的工单编号 |
| `title` | VARCHAR(200) | NOT NULL | 工单标题 |
| `description` | TEXT | NOT NULL | 问题描述 |
| `category_id` | BIGINT | NOT NULL, FK | 工单分类 |
| `requester_id` | BIGINT | NOT NULL, FK | 提交人 |
| `requester_department_id` | BIGINT | NOT NULL, FK | 提交时的部门快照 |
| `assignee_id` | BIGINT | NULL, FK | 当前处理人 |
| `status` | VARCHAR(32) | NOT NULL | 当前状态 |
| `version` | INT | NOT NULL, DEFAULT 0 | 乐观并发版本号 |
| `resolved_time` | DATETIME | NULL | 最近一次提交解决结果时间 |
| `closed_time` | DATETIME | NULL | 最终关闭时间 |
| `create_time` | DATETIME | NOT NULL | 创建时间 |
| `update_time` | DATETIME | NOT NULL | 最后业务更新时间 |

外键：

- `category_id -> ticket_category.id`；
- `requester_id -> sys_user.id`；
- `requester_department_id -> sys_department.id`；
- `assignee_id -> sys_user.id`。

索引：

- 唯一索引 `uk_ticket_no(ticket_no)`；
- 普通索引 `idx_ticket_requester_create(requester_id, create_time)`；
- 普通索引 `idx_ticket_assignee_status_update(assignee_id, status, update_time)`；
- 普通索引 `idx_ticket_status_create(status, create_time)`；
- 普通索引 `idx_ticket_category_status(category_id, status)`。

说明：`requester_department_id` 保存提交当时的部门，即使员工以后调岗，历史统计仍归属于原部门。

### 4.9 `ticket_record` 工单操作记录表

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 记录 ID |
| `ticket_id` | BIGINT | NOT NULL, FK | 工单 ID |
| `operator_id` | BIGINT | NOT NULL, FK | 操作人 |
| `target_user_id` | BIGINT | NULL, FK | 分派、改派时的目标用户 |
| `action` | VARCHAR(32) | NOT NULL | 操作类型 |
| `from_status` | VARCHAR(32) | NULL | 操作前状态 |
| `to_status` | VARCHAR(32) | NULL | 操作后状态 |
| `content` | TEXT | NOT NULL | 处理说明、解决方案或退回原因 |
| `create_time` | DATETIME | NOT NULL | 操作时间 |

外键与索引：

- `ticket_id -> ticket.id`；
- `operator_id -> sys_user.id`；
- `target_user_id -> sys_user.id`；
- 普通索引 `idx_ticket_record_ticket_time(ticket_id, create_time)`；
- 普通索引 `idx_ticket_record_operator_time(operator_id, create_time)`。

## 5. 事务和并发规则

### 5.1 状态转换必须使用条件更新

状态转换不能采用“先查询、再无条件更新”。Service 应执行类似条件：

```sql
UPDATE ticket
SET status = :toStatus,
    version = version + 1,
    update_time = :now
WHERE id = :ticketId
  AND status = :fromStatus
  AND version = :version;
```

受影响行数为 `0` 时，表示状态已改变或版本冲突，接口返回 HTTP `409 Conflict`。

### 5.2 主表与流水必须在同一事务中

一次业务动作至少包含：

1. 校验登录用户、权限和数据范围；
2. 校验当前状态与操作人；
3. 条件更新 `ticket`；
4. 插入一条 `ticket_record`；
5. 同一事务统一提交或回滚。

不能出现“状态已经变化但没有操作记录”或“记录已写入但状态没有变化”。

## 6. 数据完整性规则

- 停用用户不能登录，也不能成为新的工单处理人；
- 停用部门、角色、权限和分类不能用于新的业务数据；
- 只有启用且拥有技术人员角色的用户才能被分派；
- `PENDING` 工单的 `assignee_id` 必须为空；
- `ASSIGNED`、`PROCESSING`、`WAIT_CONFIRM` 和 `CLOSED` 工单必须存在处理人；
- `CLOSED` 工单必须存在 `closed_time`；
- 所有业务状态改变必须生成 `ticket_record`；
- 工单编号只用于展示和检索，内部关联始终使用主键 `id`。

以上跨字段规则由 Service 层负责，必要时可增加数据库检查约束，但不能只依靠前端。

## 7. 现有原型表的兼容问题

当前项目使用 `user` 表，并包含 `id`、`name`、`age`、`email`、`create_time`、`update_time`。目标模型改为 `sys_user` 后存在以下变化：

- 新增部门、登录账号、密码哈希、手机号和启停状态；
- 删除不属于当前业务的 `age`；
- 用户角色改为关联表；
- 登录方式由演示性的用户 ID 登录改为账号密码登录；
- 现有 `/user` 接口和实体不能直接作为正式 V1.0 契约继续使用。

进入迁移实现前必须先备份本地开发数据，并在以下方案中选定一种：

1. 本地原型数据无保留价值：新建目标结构并重新导入测试数据；
2. 需要保留原型数据：编写一次性迁移脚本，将旧用户映射到默认部门，并为账号初始化安全的激活或重置流程。

本文不执行删除、改表或数据搬迁。

## 8. Flyway 迁移计划草案

评审通过后建议引入 Flyway，并按只增不改的方式管理脚本：

| 版本 | 目标 |
| --- | --- |
| `V1__create_system_tables.sql` | 创建部门、用户、角色、权限及关联表 |
| `V2__create_ticket_tables.sql` | 创建分类、工单和工单记录表 |
| `V3__seed_roles_permissions.sql` | 初始化角色、权限及角色权限关系 |
| `V4__seed_dev_data.sql` | 仅在明确的开发环境初始化演示数据 |

迁移脚本一旦在共享环境执行，不得修改旧文件；后续调整必须新增版本。生产数据迁移和开发演示数据应分离，禁止在正式环境写入默认弱密码。

## 9. 已冻结决策

- V1.0 使用本地用户名和密码登录，密码采用 BCrypt 哈希，禁止保存明文；
- 旧 `user` 表作为学习原型保留，但数据不迁移到 `sys_user`；
- 用户名必填且唯一；邮箱选填，填写后必须唯一；
- V1.0 使用一级部门，不设计部门树；
- 工单编号采用 `TK + 日期 + 随机序列`，数据库唯一索引最终兜底；
- V1.0 不增加优先级、期望完成时间和紧急程度；
- Flyway 对现有非空开发库使用基线版本 `0`，不删除旧表，从 V1 开始创建目标结构；
- 不在迁移脚本中预置带弱密码的管理员账号。
