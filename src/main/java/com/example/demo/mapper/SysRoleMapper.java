package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.SysRole;
import com.example.demo.mapper.model.PermissionReferenceRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
    List<PermissionReferenceRow> selectPermissionsByRoleId(@Param("roleId") Long roleId);

    int deleteRolePermissions(@Param("roleId") Long roleId);

    int insertRolePermission(
            @Param("roleId") Long roleId,
            @Param("permissionId") Long permissionId,
            @Param("createBy") Long createBy
    );

    long countEnabledUsersByRoleId(@Param("roleId") Long roleId);

    long countUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);
}
