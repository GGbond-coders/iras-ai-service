/**
 * @file DiagnosisController.java
 * @description 诊断历史记录控制器。
 *              提供诊断记录的查询、详情和删除接口。
 *
 * @author IRAS Team
 * @since 1.0
 */
package com.iras.controller;

import com.iras.dto.PageResult;
import com.iras.dto.Result;
import com.iras.entity.DiagnosisRecord;
import com.iras.entity.User;
import com.iras.mapper.UserMapper;
import com.iras.service.DiagnosisService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/diagnosis")
@RequiredArgsConstructor
public class DiagnosisController {

    private final DiagnosisService diagnosisService;
    private final UserMapper userMapper;

    /**
     * 获取当前用户的诊断历史（分页）。
     */
    @GetMapping("/history")
    public Result<PageResult<DiagnosisRecord>> getHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        PageResult<DiagnosisRecord> result = diagnosisService.getHistory(userId, page, size);
        return Result.success(result);
    }

    /**
     * 获取诊断记录详情。
     */
    @GetMapping("/detail/{id}")
    public Result<DiagnosisRecord> getDetail(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserId(authentication);
        DiagnosisRecord record = diagnosisService.getDetail(id, userId);
        if (record == null) {
            return Result.error(404, "记录不存在");
        }
        return Result.success(record);
    }

    /**
     * 删除诊断记录。
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteRecord(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserId(authentication);
        if (diagnosisService.deleteRecord(id, userId)) {
            return Result.success("删除成功", null);
        }
        return Result.error(400, "删除失败");
    }

    /**
     * 从认证信息中获取用户 ID。
     */
    private Long getUserId(Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        User user = userMapper.findByUsername(username);
        return user.getId();
    }
}
