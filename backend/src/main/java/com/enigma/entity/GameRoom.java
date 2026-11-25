package com.enigma.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GameRoom {

    private Long id;
    private Long scriptId;
    private Long currentStageId;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
}
