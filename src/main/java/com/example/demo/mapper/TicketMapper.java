package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.Ticket;
import com.example.demo.mapper.model.TicketQueryCriteria;
import com.example.demo.mapper.model.TicketRecordRow;
import com.example.demo.mapper.model.TicketViewRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.time.LocalDateTime;

@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {
    long countByCriteria(@Param("criteria") TicketQueryCriteria criteria);

    List<TicketViewRow> selectPageByCriteria(
            @Param("criteria") TicketQueryCriteria criteria,
            @Param("offset") long offset,
            @Param("limit") long limit
    );

    TicketViewRow selectDetailById(@Param("id") Long id);

    List<TicketRecordRow> selectRecordsByTicketId(@Param("ticketId") Long ticketId);

    int updateAssignment(
            @Param("id") Long id,
            @Param("fromStatus") String fromStatus,
            @Param("version") Integer version,
            @Param("toStatus") String toStatus,
            @Param("assigneeId") Long assigneeId
    );

    int updateStatus(
            @Param("id") Long id,
            @Param("fromStatus") String fromStatus,
            @Param("version") Integer version,
            @Param("toStatus") String toStatus,
            @Param("resolvedTime") LocalDateTime resolvedTime,
            @Param("closedTime") LocalDateTime closedTime
    );
}
