package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.Ticket;
import com.example.demo.mapper.model.TicketQueryCriteria;
import com.example.demo.mapper.model.TicketRecordRow;
import com.example.demo.mapper.model.TicketViewRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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
}
