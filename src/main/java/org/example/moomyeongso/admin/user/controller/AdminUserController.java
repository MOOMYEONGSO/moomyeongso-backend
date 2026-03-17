package org.example.moomyeongso.admin.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.moomyeongso.admin.user.dto.request.AdminUserRequestDto;
import org.example.moomyeongso.admin.user.dto.response.AdminUserResponseDto;
import org.example.moomyeongso.admin.user.service.AdminUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin User API", description = "관리자 전용 유저 관리 API")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "유저 목록 조회", description = "관리자가 전체 유저 목록을 조회합니다.")
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponseDto>> getUsers() {
        return ResponseEntity.ok(adminUserService.getAllUsers());
    }

    @Operation(summary = "유저 상세 조회", description = "관리자가 특정 유저의 상세 정보를 조회합니다.")
    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminUserResponseDto> getUser(@PathVariable String userId) {
        return ResponseEntity.ok(adminUserService.getUserById(userId));
    }

    @Operation(summary = "유저 상세 조회 (닉네임)", description = "관리자가 특정 유저의 상세 정보를 닉네임으로 조회합니다. (USER만 대상)")
    @GetMapping("/users/nickname/{nickname}")
    public ResponseEntity<AdminUserResponseDto> getUserByNickname(@PathVariable String nickname) {
        return ResponseEntity.ok(adminUserService.getUserByNickname(nickname));
    }

    @Operation(summary = "유저 상태 변경", description = "관리자가 특정 유저의 상태를 변경합니다.")
    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<Void> updateUserStatus(
            @PathVariable String userId,
            @Valid
            @RequestBody AdminUserRequestDto request
    ) {
        adminUserService.updateUserStatus(userId, request);
        return ResponseEntity.noContent().build();
    }
}
