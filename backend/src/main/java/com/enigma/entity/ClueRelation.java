package com.enigma.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClueRelation {

    private Long id;
    private Long scriptId;
    private Long clueId;
    private String answerField;
    private String relationType;
    private Integer score;
    private LocalDateTime createTime;
}
