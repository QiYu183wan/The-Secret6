package com.enigma.controller;

import com.enigma.common.R;
import com.enigma.dto.RequestDtos;
import com.enigma.dto.ViewDtos;
import com.enigma.security.CurrentUserService;
import com.enigma.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final CurrentUserService currentUser;

    @GetMapping("/{roomId}")
    public R<ViewDtos.RoomInfo> room(@PathVariable Long roomId) {
        return R.ok(roomService.getRoomInfo(roomId, currentUser.id()));
    }

    @GetMapping("/{roomId}/character")
    public R<ViewDtos.MyCharacter> character(@PathVariable Long roomId) {
        return R.ok(roomService.getMyCharacter(roomId, currentUser.id()));
    }

    @GetMapping("/{roomId}/stage")
    public R<ViewDtos.StageProgress> stage(@PathVariable Long roomId) {
        return R.ok(roomService.getStageProgress(roomId, currentUser.id()));
    }

    @GetMapping("/{roomId}/clues")
    public R<List<ViewDtos.ClueItem>> clues(@PathVariable Long roomId) {
        return R.ok(roomService.getClues(roomId, currentUser.id()));
    }

    @GetMapping("/{roomId}/evidence-chain")
    public R<ViewDtos.EvidenceChain> evidenceChain(@PathVariable Long roomId) {
        return R.ok(roomService.getEvidenceChain(roomId, currentUser.id()));
    }

    @PostMapping("/{roomId}/submission")
    public R<ViewDtos.SubmissionResult> submit(@PathVariable Long roomId,
                                               @Valid @RequestBody RequestDtos.SubmissionRequest request) {
        return R.ok(roomService.submit(roomId, currentUser.id(), request));
    }

    @GetMapping("/{roomId}/result")
    public R<ViewDtos.ResultInfo> result(@PathVariable Long roomId) {
        return R.ok(roomService.getResult(roomId, currentUser.id()));
    }

    @GetMapping("/{roomId}/replay")
    public R<ViewDtos.ReplayInfo> replay(@PathVariable Long roomId) {
        return R.ok(roomService.getReplay(roomId, currentUser.id()));
    }
}
