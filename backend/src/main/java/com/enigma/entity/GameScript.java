package com.enigma.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GameScript {

    private Long id;
    private String title;
    private String description;
    private String story;
    private String difficulty;
    private Integer minPlayers;
    private Integer maxPlayers;
    private Integer requiredPlayers;
    private Integer estimatedMinutes;
    private String status;
    private String coverImage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
