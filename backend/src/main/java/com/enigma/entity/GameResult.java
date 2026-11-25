package com.enigma.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GameResult {

    private Long id;
    private Long roomId;
    private Long userId;
    private Long characterId;
    private String finalAnswer;
    private Integer score;
    private String scoreDetail;
    private String result;
    private Integer hiddenGoal;
    private LocalDateTime createTime;
}
