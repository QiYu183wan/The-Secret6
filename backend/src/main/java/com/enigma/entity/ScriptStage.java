package com.enigma.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScriptStage {

    private Long id;
    private Long scriptId;
    private String stageName;
    private String stageDescription;
    private Integer stageOrder;
    private String unlockRule;
    private Integer timeoutMinutes;
    private LocalDateTime createTime;
}
