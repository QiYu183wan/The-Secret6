package com.enigma.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GameSubmission {

    private Long id;
    private Long roomId;
    private Long userId;
    private String characterAnswer;
    private String timeAnswer;
    private String locationAnswer;
    private String actionAnswer;
    private String explanation;
    private LocalDateTime submitTime;
    private Integer score;
    private String scoreDetail;
    private Integer submissionOrder;
}
