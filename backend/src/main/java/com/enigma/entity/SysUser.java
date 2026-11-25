package com.enigma.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysUser {

    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String bio;
    private String role;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
