-- V1.0 工单表：分类、工单主表和只追加的操作记录表。

CREATE TABLE ticket_category (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    code VARCHAR(50) NOT NULL COMMENT '分类编码',
    name VARCHAR(100) NOT NULL COMMENT '分类名称',
    description VARCHAR(255) NULL COMMENT '分类说明',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    create_by BIGINT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT NULL COMMENT '最后修改人ID',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_category_code (code),
    KEY idx_ticket_category_status_sort (status, sort_order),
    CONSTRAINT chk_ticket_category_status CHECK (status IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单分类表';

CREATE TABLE ticket (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '工单ID',
    ticket_no VARCHAR(32) NOT NULL COMMENT '工单编号',
    title VARCHAR(200) NOT NULL COMMENT '工单标题',
    description TEXT NOT NULL COMMENT '问题描述',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    requester_id BIGINT NOT NULL COMMENT '提交人ID',
    requester_department_id BIGINT NOT NULL COMMENT '提交时部门ID快照',
    assignee_id BIGINT NULL COMMENT '当前处理人ID',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '工单状态',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    resolved_time DATETIME NULL COMMENT '最近提交解决结果时间',
    closed_time DATETIME NULL COMMENT '最终关闭时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后业务更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_no (ticket_no),
    KEY idx_ticket_requester_create (requester_id, create_time),
    KEY idx_ticket_assignee_status_update (assignee_id, status, update_time),
    KEY idx_ticket_status_create (status, create_time),
    KEY idx_ticket_category_status (category_id, status),
    CONSTRAINT fk_ticket_category FOREIGN KEY (category_id) REFERENCES ticket_category (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ticket_requester FOREIGN KEY (requester_id) REFERENCES sys_user (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ticket_requester_department FOREIGN KEY (requester_department_id) REFERENCES sys_department (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ticket_assignee FOREIGN KEY (assignee_id) REFERENCES sys_user (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_ticket_status CHECK (status IN ('PENDING', 'ASSIGNED', 'PROCESSING', 'WAIT_CONFIRM', 'CLOSED')),
    CONSTRAINT chk_ticket_version CHECK (version >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单主表';

CREATE TABLE ticket_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    ticket_id BIGINT NOT NULL COMMENT '工单ID',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    target_user_id BIGINT NULL COMMENT '分派或改派的目标用户ID',
    action VARCHAR(32) NOT NULL COMMENT '操作类型',
    from_status VARCHAR(32) NULL COMMENT '操作前状态',
    to_status VARCHAR(32) NULL COMMENT '操作后状态',
    content TEXT NOT NULL COMMENT '操作说明、解决方案或退回原因',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_ticket_record_ticket_time (ticket_id, create_time),
    KEY idx_ticket_record_operator_time (operator_id, create_time),
    CONSTRAINT fk_ticket_record_ticket FOREIGN KEY (ticket_id) REFERENCES ticket (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ticket_record_operator FOREIGN KEY (operator_id) REFERENCES sys_user (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ticket_record_target_user FOREIGN KEY (target_user_id) REFERENCES sys_user (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_ticket_record_action CHECK (action IN ('CREATE', 'ASSIGN', 'REASSIGN', 'START', 'ADD_RECORD', 'RESOLVE', 'CONFIRM', 'RETURN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单操作记录表';
