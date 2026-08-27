package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 正式用户及其 RBAC 查询入口。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    SysUser selectByUsername(@Param("username") String username);

    String selectEnabledDepartmentNameById(@Param("departmentId") Long departmentId);

    Long selectEnabledDepartmentIdByCode(@Param("code") String code);

    Long selectEnabledRoleIdByCode(@Param("code") String code);

    long countUsersByRoleCode(@Param("roleCode") String roleCode);

    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    List<String> selectEnabledRoleCodesByUserId(@Param("userId") Long userId);

    List<String> selectEnabledPermissionCodesByUserId(@Param("userId") Long userId);
}
