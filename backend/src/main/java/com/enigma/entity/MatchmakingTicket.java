package com.enigma.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MatchmakingTicket {

    private Long id;
    private Long userId;
    private Long scriptId;
    private String status;
    private Long matchedRoomId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
