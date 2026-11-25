package com.enigma.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PlayerClue {

    private Long id;
    private Long roomId;
    private Long userId;
    private Long clueId;
    private Integer isRead;
    private LocalDateTime acquireTime;
}
