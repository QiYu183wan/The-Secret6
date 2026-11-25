package com.enigma.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoomStageRecord {

    private Long id;
    private Long roomId;
    private Long stageId;
    private LocalDateTime unlockTime;
    private String unlockReason;
    private Integer isOpen;
}
