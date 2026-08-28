package com.example.demo.converter;

import com.example.demo.dto.ReferenceItemResponse;
import com.example.demo.dto.UserAdminResponse;
import com.example.demo.entity.SysDepartment;
import com.example.demo.entity.SysUser;
import com.example.demo.mapper.model.RoleReferenceRow;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserAdminConverter {
    public UserAdminResponse toResponse(
            SysUser user,
            SysDepartment department,
            List<RoleReferenceRow> roles) {
        List<ReferenceItemResponse> roleItems = roles.stream()
                .map(role -> new ReferenceItemResponse(
                        String.valueOf(role.id()), role.code(), role.name()))
                .toList();
        return new UserAdminResponse(
                String.valueOf(user.getId()), String.valueOf(user.getDepartmentId()),
                department == null ? null : department.getName(), user.getUsername(), user.getName(),
                user.getEmail(), user.getMobile(), user.getStatus(), roleItems,
                user.getCreateTime(), user.getUpdateTime()
        );
    }
}
