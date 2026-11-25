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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RoomService roomService;
    private final CurrentUserService currentUser;

    @GetMapping("/api/user/profile")
    public R<ViewDtos.UserInfo> profile() {
        return R.ok(userService.toInfo(currentUser.get()));
    }

    @PutMapping("/api/user/profile")
    public R<ViewDtos.UserInfo> updateProfile(@Valid @RequestBody RequestDtos.ProfileRequest request) {
        return R.ok(userService.updateProfile(currentUser.id(), request));
    }

    @PutMapping("/api/user/password")
    public R<Void> changePassword(@Valid @RequestBody RequestDtos.PasswordRequest request) {
        userService.changePassword(currentUser.id(), request);
        return R.ok();
    }

    @GetMapping("/api/user/history")
    public R<List<ViewDtos.HistoryItem>> history() {
        return R.ok(roomService.history(currentUser.id()));
    }
}
