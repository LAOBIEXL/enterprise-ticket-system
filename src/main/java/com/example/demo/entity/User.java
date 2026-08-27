package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("`user`")
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "User", description = "用户信息")
public class User {
    @TableId(type = IdType.AUTO)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "用户主键 ID，由数据库自动生成", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "用户姓名", example = "Jack")
    private String name;

    @Schema(description = "用户年龄", example = "20")
    private Integer age;

    @Schema(description = "用户邮箱", example = "jack@example.com")
    private String email;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "2026-08-27 11:43:07", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后更新时间", example = "2026-08-27 11:43:07", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updateTime;
}
