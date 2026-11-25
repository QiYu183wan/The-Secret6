package com.enigma.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScriptCharacter {

    private Long id;
    private Long scriptId;
    private String characterName;
    private String description;
    private String characterGoal;
    private Integer isHidden;
    private Integer sortOrder;
    private LocalDateTime createTime;
}
