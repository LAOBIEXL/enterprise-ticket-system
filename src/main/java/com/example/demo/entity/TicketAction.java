package com.example.demo.entity;

/** 工单审计流水的操作类型。 */
public enum TicketAction {
    CREATE,
    ASSIGN,
    REASSIGN,
    START,
    ADD_RECORD,
    RESOLVE,
    CONFIRM,
    RETURN
}
