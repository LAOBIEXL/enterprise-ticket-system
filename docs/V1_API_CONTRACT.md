# 企业内部工单系统 V1.0 API 契约

> 状态：已确认内部开发基线
> 依据：[V1 需求与概要设计](./V1_REQUIREMENTS_AND_DESIGN.md)
> 数据模型：[V1 数据库设计](./V1_DATABASE_DESIGN.md)
> 说明：本文描述目标接口，不代表接口已经实现。

## 1. 契约原则

- Controller 负责接收参数、参数校验、调用 Service 和返回结果；
- Service 负责业务规则、状态转换、事务和并发控制；
- Mapper 只负责数据库访问；
- 请求和响应使用 DTO，不直接暴露 Entity 或 MyBatis-Plus `Page`；
- HTTP 状态码表达请求结果，响应体中的 `code` 与其保持一致；
- 受保护接口同时进行登录校验、功能权限校验和数据范围校验；
- 工单业务动作采用明确的命令接口，不提供任意修改 `status` 的通用接口。

## 2. 基础约定

### 2.1 路径与请求头

本文只写业务路径，例如 `/tickets`。当前开发环境配置了 `/dev-api` 上下文路径，实际开发地址为 `http://localhost:8080/dev-api/tickets`。

- JSON 请求使用 `Content-Type: application/json`；
- 登录后的请求使用 `satoken: <tokenValue>`；
- 创建工单携带 `Idempotency-Key: <UUID>`，防止网络重试产生重复工单；
- 时间使用 ISO 8601 格式，例如 `2026-08-27T18:30:00`；
- Java ID 类型统一为 `Long`，响应时可序列化为字符串，避免 JavaScript 大整数精度丢失。

### 2.2 统一响应体

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

Java 类型统一为：

- 成功且有数据：`Result<T>`；
- 成功且无数据：`Result<Void>`；
- 分页结果：`Result<PageResponse<T>>`；
- 错误时 `data` 为 `null`。

`PageResponse<T>` 固定字段：

```json
{
  "records": [],
  "total": 0,
  "pageNum": 1,
  "pageSize": 10,
  "pages": 0
}
```

分页参数 `pageNum` 默认 1、最小 1；`pageSize` 默认 10、范围 1 到 100。

### 2.3 HTTP 状态与错误

| HTTP 状态 | code | 使用场景 |
| --- | ---: | --- |
| 200 | 200 | 查询、更新或业务动作成功 |
| 201 | 201 | 资源创建成功 |
| 400 | 400 | 参数格式错误、字段校验失败 |
| 401 | 401 | 未登录、Token 无效或过期 |
| 403 | 403 | 没有功能权限或数据访问权 |
| 404 | 404 | 用户、工单、分类等资源不存在 |
| 409 | 409 | 状态转换非法、版本冲突、唯一值重复 |
| 500 | 500 | 未预期的服务端异常 |

生产环境的 `500` 响应不能暴露堆栈、SQL、密码、Token 或内部类名；详细信息只写服务端日志并携带追踪 ID。

## 3. 权限与数据范围

### 3.1 权限编码

| 权限编码 | 能力 |
| --- | --- |
| `ticket:create` | 创建工单 |
| `ticket:read:own` | 查看本人提交的工单 |
| `ticket:read:assigned` | 查看分派给本人的工单 |
| `ticket:read:all` | 查看全部工单 |
| `ticket:assign` | 首次分派工单 |
| `ticket:reassign` | 改派工单 |
| `ticket:start` | 开始处理 |
| `ticket:record:add` | 添加处理记录 |
| `ticket:resolve` | 提交解决结果 |
| `ticket:confirm` | 确认或退回本人提交的工单 |
| `department:manage` | 管理部门 |
| `user:manage` | 管理用户和用户角色 |
| `role:manage` | 管理角色及角色权限 |
| `permission:manage` | 查看和维护权限定义 |
| `ticket:category:manage` | 管理工单分类 |

### 3.2 数据范围规则

- 员工只能查看本人提交的工单；
- 技术人员只能处理当前分派给自己的工单；
- 提交人只能确认或退回本人处于 `WAIT_CONFIRM` 的工单；
- 支持主管拥有 `ticket:read:all` 后可以查看全部工单；
- 系统管理员管理基础数据，但默认不自动获得工单数据权限；
- 前端隐藏按钮只是交互优化，后端仍必须逐个接口验证权限和数据范围。

## 4. 认证接口

### 4.1 登录

`POST /auth/login`

`LoginRequest`：

```json
{
  "username": "zhangsan",
  "password": "example-password"
}
```

- `username`：必填，1 到 64 个字符；
- `password`：必填，8 到 64 个字符，禁止写入请求日志；
- 成功响应：`Result<LoginResponse>`。

```json
{
  "code": 200,
  "msg": "登录成功",
  "data": {
    "tokenName": "satoken",
    "tokenValue": "masked-token-value",
    "user": {
      "id": "1",
      "username": "zhangsan",
      "name": "张三",
      "departmentName": "行政部",
      "roles": ["EMPLOYEE"]
    }
  }
}
```

服务端调用 Sa-Token 登录后，会话写入 Redis。前端保存 `tokenValue`，后续主动放入 `satoken` 请求头；Swagger UI 使用 Authorize 统一设置该请求头。

### 4.2 退出、当前用户与登录检查

| 方法与路径 | 登录 | 响应 | 说明 |
| --- | --- | --- | --- |
| `POST /auth/logout` | 是 | `Result<Void>` | 注销当前 Token |
| `GET /auth/me` | 是 | `Result<CurrentUserResponse>` | 返回用户、角色和权限，正式前端主要使用此接口 |
| `GET /auth/is-login` | 否 | `Result<LoginStatusResponse>` | 开发调试用登录状态检查 |

## 5. 工单 DTO

### 5.1 请求 DTO

| DTO | 字段与规则 |
| --- | --- |
| `CreateTicketRequest` | `categoryId` 必填；`title` 1~200；`description` 1~5000 |
| `AssignTicketRequest` | `assigneeId` 必填；`remark` 可选、最长 500 |
| `ReassignTicketRequest` | `assigneeId` 必填；`reason` 1~500 |
| `AddTicketRecordRequest` | `content` 1~2000 |
| `ResolveTicketRequest` | `solution` 1~5000 |
| `ConfirmTicketRequest` | `remark` 可选、最长 500 |
| `ReturnTicketRequest` | `reason` 1~1000 |

创建工单时，`requesterId`、`departmentId`、`status` 和 `assigneeId` 均由服务端确定，客户端不能提交这些字段。

### 5.2 `TicketQuery`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `status` | String | 工单状态，可选 |
| `categoryId` | Long | 分类 ID，可选 |
| `requesterId` | Long | 提交人，仅全部工单查询可用 |
| `assigneeId` | Long | 处理人，仅全部工单查询可用 |
| `keyword` | String | 匹配工单编号或标题，最长 100 |
| `pageNum` | Integer | 页码 |
| `pageSize` | Integer | 每页数量 |

默认按 `createTime` 倒序。V1.0 不接受客户端传入任意数据库列名作为排序字段。

### 5.3 响应 DTO

`TicketSummaryResponse` 用于列表，包含：

- `id`、`ticketNo`、`title`、`status`；
- 分类 ID 和名称；
- 提交人 ID、姓名、提交时部门名称；
- 当前处理人 ID、姓名，可为空；
- `createTime`、`updateTime`。

`TicketDetailResponse` 在摘要基础上增加 `description`、`resolvedTime`、`closedTime` 和 `records`。

`TicketRecordResponse` 包含 `id`、`action`、`fromStatus`、`toStatus`、`content`、操作人摘要、目标用户摘要和 `createTime`。

## 6. 工单接口

### 6.1 查询和创建

| 方法与路径 | 权限 | 响应 | 说明 |
| --- | --- | --- | --- |
| `POST /tickets` | `ticket:create` | 201，`Result<TicketDetailResponse>` | 创建 `PENDING` 工单并写入 `CREATE` 记录 |
| `GET /tickets/mine` | `ticket:read:own` | `Result<PageResponse<TicketSummaryResponse>>` | 查询本人提交的工单 |
| `GET /tickets/assigned` | `ticket:read:assigned` | 同上 | 查询当前分派给本人的工单 |
| `GET /tickets` | `ticket:read:all` | 同上 | 主管分页查询全部工单 |
| `GET /tickets/{id}` | 任一读取权限 | `Result<TicketDetailResponse>` | 还需验证提交人、处理人或全量数据范围 |

创建接口要求 `Idempotency-Key`。服务端在 Redis 中按“用户 ID + 幂等键”保存 24 小时；重复请求返回第一次创建的工单，不重复插入。

### 6.2 状态动作

| 方法与路径 | 权限 | 合法状态 | 请求 DTO | 响应 |
| --- | --- | --- | --- | --- |
| `POST /tickets/{id}/assign` | `ticket:assign` | `PENDING -> ASSIGNED` | `AssignTicketRequest` | `Result<TicketDetailResponse>` |
| `POST /tickets/{id}/reassign` | `ticket:reassign` | `ASSIGNED -> ASSIGNED` 或 `PROCESSING -> ASSIGNED` | `ReassignTicketRequest` | 同上 |
| `POST /tickets/{id}/start` | `ticket:start` | `ASSIGNED -> PROCESSING` | 无 | 同上 |
| `POST /tickets/{id}/records` | `ticket:record:add` | `PROCESSING -> PROCESSING` | `AddTicketRecordRequest` | 同上 |
| `POST /tickets/{id}/resolve` | `ticket:resolve` | `PROCESSING -> WAIT_CONFIRM` | `ResolveTicketRequest` | 同上 |
| `POST /tickets/{id}/confirm` | `ticket:confirm` | `WAIT_CONFIRM -> CLOSED` | `ConfirmTicketRequest` | 同上 |
| `POST /tickets/{id}/return` | `ticket:confirm` | `WAIT_CONFIRM -> PROCESSING` | `ReturnTicketRequest` | 同上 |

补充约束：

- 分派和改派的目标用户必须启用并拥有技术人员角色；
- 改派目标不能与当前处理人相同；
- 开始、记录和解决只能由当前处理人执行；
- 确认和退回只能由工单提交人执行；
- 每个动作都要条件更新主表并在同一事务中追加 `ticket_record`；
- 条件更新受影响行数为 0 时返回 HTTP `409`。

并发冲突示例：

```http
HTTP/1.1 409 Conflict
Content-Type: application/json

{
  "code": 409,
  "msg": "工单状态已变化，请刷新后重试",
  "data": null
}
```

## 7. 基础数据管理接口

V1.0 使用新增、修改和启停，不提供物理删除接口。

### 7.1 部门

| 方法与路径 | 权限 | 用途 |
| --- | --- | --- |
| `GET /departments` | 已登录 | 查询启用部门列表 |
| `GET /admin/departments` | `department:manage` | 分页查询全部部门 |
| `POST /admin/departments` | `department:manage` | 新增部门 |
| `PUT /admin/departments/{id}` | `department:manage` | 修改部门 |
| `PATCH /admin/departments/{id}/status` | `department:manage` | 启用或停用部门 |

`GET /departments` 仅返回启用数据，并按 `sortOrder`、`id` 升序排列。列表项使用统一的轻量结构：

```json
{
  "id": "5",
  "code": "TECHNOLOGY",
  "name": "技术部"
}
```

### 7.2 用户

| 方法与路径 | 权限 | 用途 |
| --- | --- | --- |
| `GET /admin/users` | `user:manage` | 分页查询用户 |
| `GET /admin/users/{id}` | `user:manage` | 查询用户详情 |
| `POST /admin/users` | `user:manage` | 创建用户 |
| `PUT /admin/users/{id}` | `user:manage` | 修改部门、姓名、邮箱和手机号 |
| `PATCH /admin/users/{id}/status` | `user:manage` | 启用或停用用户 |
| `PUT /admin/users/{id}/roles` | `user:manage` | 用角色 ID 集合替换现有角色 |
| `PUT /admin/users/{id}/password` | `user:manage` | 管理员重置密码 |

`CreateUserRequest` 包含 `departmentId`、`username`、`initialPassword`、`name`、`email` 和 `mobile`。密码只接收一次，哈希后入库，不在响应中返回。

### 7.3 角色、权限和分类

| 方法与路径 | 权限 | 用途 |
| --- | --- | --- |
| `GET /admin/roles` | `role:manage` | 查询角色 |
| `POST /admin/roles` | `role:manage` | 创建角色 |
| `PUT /admin/roles/{id}` | `role:manage` | 修改角色 |
| `PATCH /admin/roles/{id}/status` | `role:manage` | 启停角色 |
| `PUT /admin/roles/{id}/permissions` | `role:manage` | 替换角色权限集合 |
| `GET /admin/permissions` | `permission:manage` | 查询权限定义 |
| `GET /ticket-categories` | 已登录 | 查询启用分类 |
| `GET /admin/ticket-categories` | `ticket:category:manage` | 分页查询全部分类 |
| `POST /admin/ticket-categories` | `ticket:category:manage` | 新增分类 |
| `PUT /admin/ticket-categories/{id}` | `ticket:category:manage` | 修改分类 |
| `PATCH /admin/ticket-categories/{id}/status` | `ticket:category:manage` | 启停分类 |

`GET /ticket-categories` 与部门列表使用相同的轻量结构，只返回启用分类，并按 `sortOrder`、`id` 升序排列。

管理端分类分页查询参数为 `keyword`、`status`、`pageNum` 和 `pageSize`。`keyword` 模糊匹配编码或名称，`status` 只能为 `0` 或 `1`，每页最多 100 条。

新增和修改请求包含 `code`、`name`、`description`、`sortOrder`；分类编码统一转为大写并保证唯一。新增分类默认启用，成功返回 HTTP 201；编码冲突返回 HTTP 409；目标分类不存在返回 HTTP 404。启停请求体如下：

```json
{
  "status": 0
}
```

V1.0 不提供分类物理删除接口；已被历史工单引用的分类应通过停用退出可选列表。

权限编码由后端业务能力定义，V1.0 不允许管理员创建一个代码中不存在的权限编码。

## 8. 业务动作路径与 REST

普通资源操作优先使用资源语义，例如 `GET /tickets/{id}`、`POST /tickets` 和 `PUT /admin/users/{id}`。

“分派、开始处理、解决、确认、退回”则不是任意字段更新，而是带权限、前置状态、事务和审计的业务命令。使用 `/assign`、`/start`、`/resolve` 能明确意图，并避免暴露以下危险接口：

```text
PATCH /tickets/{id}  body: { "status": "CLOSED" }
```

业务命令路径是 REST 风格在复杂业务中的常见扩展。它仍然作用于明确的工单资源，但由 Service 严格验证状态转换。

## 9. 当前原型的迁移影响

- 原型登录按用户 ID，目标登录按账号密码；
- 原型 `/user` 接口直接使用 Entity，目标 `/admin/users` 使用 DTO；
- 原型分页可能暴露 MyBatis-Plus 分页对象，目标统一返回 `PageResponse<T>`；
- 目标用户模型不再包含 `age`；
- 实现时需要明确旧接口下线或兼容策略，不能让两套含义不同的接口长期并存。

## 10. OpenAPI 文档要求

- Controller 类使用一个 `@Tag`；
- 接口使用简短 `@Operation(summary = ...)`；
- DTO 仅在含义不明显、枚举或特殊约束字段上使用 `@Schema`；
- 参数规则使用 Jakarta Validation；
- `400`、`401`、`403`、`404`、`409` 和 `500` 尽量全局说明，避免每个接口重复大量注解；
- OpenAPI 配置 Sa-Token 请求头安全方案，Swagger UI 登录后统一授权。

## 11. 实现顺序

1. 评审并冻结数据库和 API 契约的未决项；
2. 引入 Flyway，创建基础数据与工单表；
3. 实现用户、角色、权限查询及 Sa-Token 权限加载；
4. 实现账号密码登录和 `/auth/me`；
5. 实现工单创建、查询和详情；
6. 实现状态机、事务流水和并发测试；
7. 实现管理端基础数据接口；
8. 补充集成测试和 OpenAPI 错误响应。

## 12. 已冻结决策

- 正式认证将现有用户 ID 登录替换为用户名和密码登录；
- 创建工单必须携带 `Idempotency-Key`，服务端使用 Redis 保存 24 小时；
- 用户邮箱选填，填写后必须唯一；
- 技术主管可以将 `PROCESSING` 工单改派，改派后状态变为 `ASSIGNED`；
- V1.0 不允许员工撤回 `PENDING` 工单，也不允许关闭后重新打开；
- 系统管理员默认没有全部工单数据权限；
- 旧 `/user` 和原型登录接口在新接口验证完成前暂时保留，随后下线，不作为正式契约。
