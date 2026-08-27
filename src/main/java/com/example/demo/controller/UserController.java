package com.example.demo.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@Tag(name = "用户管理")
@SecurityRequirement(name = "satoken")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @SaCheckPermission("user:add")
    @Operation(summary = "新增用户")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "新增成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无新增权限")
    })
    public ResponseEntity<Result<User>> save(@RequestBody User user) {
        if (userService.save(user)) {
            return ResponseEntity.ok(Result.success(user));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(500, "新增用户失败", null));
    }

    @GetMapping
    @SaCheckPermission("user:list")
    @Operation(summary = "查询全部用户")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无查询权限")
    })
    public ResponseEntity<Result<List<User>>> getAll() {
        return ResponseEntity.ok(Result.success(userService.list()));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("user:get")
    @Operation(summary = "按 ID 查询用户")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无查询权限"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    public ResponseEntity<Result<User>> getById(
            @Parameter(description = "用户 ID", example = "1")
            @PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Result.fail(404, "用户不存在", null));
        }
        return ResponseEntity.ok(Result.success(user));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("user:update")
    @Operation(summary = "更新用户")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无更新权限"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    public ResponseEntity<Result<User>> update(
            @Parameter(description = "用户 ID", example = "1")
            @PathVariable Long id,
            @RequestBody User user) {
        user.setId(id);
        if (userService.updateById(user)) {
            return ResponseEntity.ok(Result.success(user));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.fail(404, "用户不存在，更新失败", null));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("user:delete")
    @Operation(summary = "删除用户")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无删除权限"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    public ResponseEntity<Result<Void>> delete(
            @Parameter(description = "用户 ID", example = "1")
            @PathVariable Long id) {
        if (userService.removeById(id)) {
            return ResponseEntity.ok(Result.success());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.fail(404, "用户不存在，删除失败"));
    }

    @GetMapping("/page")
    @SaCheckPermission("user:list")
    @Operation(summary = "分页查询用户")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无查询权限"),
            @ApiResponse(responseCode = "400", description = "分页参数错误")
    })
    public ResponseEntity<Result<Page<User>>> getPage(
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数", example = "10")
            @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "用户姓名", example = "Jack")
            @RequestParam(required = false) String name) {
        if (pageNum < 1 || pageSize < 1) {
            return ResponseEntity.badRequest()
                    .body(Result.fail(400, "页码和每页条数必须大于0", null));
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(name != null && !name.isBlank(), User::getName, name);

        Page<User> page = userService.page(
                new Page<>(pageNum, pageSize),
                queryWrapper
        );
        return ResponseEntity.ok(Result.success(page));
    }
}
