package com.enigma.controller;

import com.enigma.common.R;
import com.enigma.dto.ViewDtos;
import com.enigma.security.CurrentUserService;
import com.enigma.service.MatchmakingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matchmaking")
@RequiredArgsConstructor
public class MatchmakingController {

    private final MatchmakingService matchmakingService;
    private final CurrentUserService currentUser;

    @PostMapping("/{scriptId}/join")
    public R<ViewDtos.MatchStatus> join(@PathVariable Long scriptId) {
        return R.ok(matchmakingService.join(scriptId, currentUser.id()));
    }

    @GetMapping("/{scriptId}/status")
    public R<ViewDtos.MatchStatus> status(@PathVariable Long scriptId) {
        return R.ok(matchmakingService.status(scriptId, currentUser.id()));
    }

    @DeleteMapping("/{scriptId}/cancel")
    public R<Void> cancel(@PathVariable Long scriptId) {
        matchmakingService.cancel(scriptId, currentUser.id());
        return R.ok();
    }
}
