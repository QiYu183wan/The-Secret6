package com.enigma.controller;

import com.enigma.common.R;
import com.enigma.dto.RequestDtos;
import com.enigma.dto.ViewDtos;
import com.enigma.security.CurrentUserService;
import com.enigma.service.RoomService;
import com.enigma.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final RoomService roomService;
    private final CurrentUserService currentUser;

    @GetMapping("/users")
    public R<List<ViewDtos.UserInfo>> users() {
        return R.ok(userService.listUsers());
    }

    @PutMapping("/users/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id,
                                @Valid @RequestBody RequestDtos.StatusRequest request) {
        userService.updateStatus(currentUser.id(), id, request.getStatus());
        return R.ok();
    }

    @PutMapping("/users/{id}/role")
    public R<Void> updateRole(@PathVariable Long id,
                              @Valid @RequestBody RequestDtos.StatusRequest request) {
        userService.updateRole(currentUser.id(), id, request.getStatus());
        return R.ok();
    }

    @GetMapping("/rooms")
    public R<List<ViewDtos.AdminRoomItem>> rooms() {
        return R.ok(roomService.adminRooms());
    }
}
