package com.enigma.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoomPlayer {

    private Long id;
    private Long roomId;
    private Long userId;
    private Long characterId;
    private LocalDateTime joinTime;
    private String status;
}
