package com.enigma.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScriptAnswer {

    private Long id;
    private Long scriptId;
    private String characterAnswer;
    private String timeAnswer;
    private String locationAnswer;
    private String actionAnswer;
    private String scoringRuleJson;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
