package com.enigma.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScriptClue {

    private Long id;
    private Long scriptId;
    private Long stageId;
    private String title;
    private String content;
    private String visibilityType;
    private Long visibleCharacterId;
    private String eventTime;
    private String location;
    private Integer credibility;
    private Integer isKeyClue;
    private Integer isMisleading;
    private Integer sortOrder;
    private LocalDateTime createTime;
}
