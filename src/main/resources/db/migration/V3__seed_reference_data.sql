-- V1.0 基础数据：部门、工单分类、角色、权限和角色权限关系。
-- 不初始化带默认弱密码的管理员账号。

INSERT INTO sys_department (code, name, status, sort_order)
VALUES ('ADMINISTRATION', '行政部', 1, 10),
       ('HR', '人事部', 1, 20),
       ('FINANCE', '财务部', 1, 30),
       ('SALES', '销售部', 1, 40),
       ('TECHNOLOGY', '技术部', 1, 50);

INSERT INTO ticket_category (code, name, description, status, sort_order)
VALUES ('COMPUTER', '电脑故障', '台式机、笔记本及外设故障', 1, 10),
       ('SOFTWARE', '软件安装', '办公软件安装、升级和使用问题', 1, 20),
       ('ACCOUNT', '账号申请', '内部系统账号申请及账号问题', 1, 30),
       ('NETWORK', '网络问题', '有线网络、无线网络和访问异常', 1, 40),
       ('OTHER', '其他问题', '不属于以上分类的技术支持问题', 1, 99);

INSERT INTO sys_role (code, name, description, status)
VALUES ('EMPLOYEE', '普通员工', '提交、查看并确认本人工单', 1),
       ('TECHNICIAN', '技术人员', '处理分派给本人的工单', 1),
       ('SUPPORT_MANAGER', '技术主管', '查看全部工单并执行分派和改派', 1),
       ('SYSTEM_ADMIN', '系统管理员', '管理用户、部门、角色、权限和工单分类', 1);

INSERT INTO sys_permission (code, name, description, status)
VALUES ('ticket:create', '创建工单', '创建本人作为提交人的工单', 1),
       ('ticket:read:own', '查看本人工单', '查看本人提交的工单', 1),
       ('ticket:read:assigned', '查看分派工单', '查看当前分派给本人的工单', 1),
       ('ticket:read:all', '查看全部工单', '查看所有员工提交的工单', 1),
       ('ticket:assign', '分派工单', '将待分派工单分配给技术人员', 1),
       ('ticket:reassign', '改派工单', '更换已分派或处理中工单的处理人', 1),
       ('ticket:start', '开始处理', '开始处理分派给本人的工单', 1),
       ('ticket:record:add', '添加处理记录', '为处理中的工单追加处理说明', 1),
       ('ticket:resolve', '提交解决结果', '提交解决方案并等待员工确认', 1),
       ('ticket:confirm', '确认工单结果', '确认或退回本人提交的工单', 1),
       ('department:manage', '部门管理', '新增、修改和启停部门', 1),
       ('user:manage', '用户管理', '新增、修改、启停用户并分配角色', 1),
       ('role:manage', '角色管理', '新增、修改、启停角色并分配权限', 1),
       ('permission:manage', '权限管理', '查看和维护系统权限定义', 1),
       ('ticket:category:manage', '工单分类管理', '新增、修改和启停工单分类', 1);

-- 普通员工权限。
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.code IN (
    'ticket:create',
    'ticket:read:own',
    'ticket:confirm'
)
WHERE r.code = 'EMPLOYEE';

-- 技术人员同时具备普通员工能力，并可以处理分派给自己的工单。
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.code IN (
    'ticket:create',
    'ticket:read:own',
    'ticket:confirm',
    'ticket:read:assigned',
    'ticket:start',
    'ticket:record:add',
    'ticket:resolve'
)
WHERE r.code = 'TECHNICIAN';

-- 技术主管拥有完整工单业务能力，但不自动获得系统管理能力。
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.code IN (
    'ticket:create',
    'ticket:read:own',
    'ticket:confirm',
    'ticket:read:assigned',
    'ticket:read:all',
    'ticket:assign',
    'ticket:reassign',
    'ticket:start',
    'ticket:record:add',
    'ticket:resolve'
)
WHERE r.code = 'SUPPORT_MANAGER';

-- 系统管理员只获得基础数据管理能力；业务工单权限需要额外角色。
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.code IN (
    'department:manage',
    'user:manage',
    'role:manage',
    'permission:manage',
    'ticket:category:manage'
)
WHERE r.code = 'SYSTEM_ADMIN';
