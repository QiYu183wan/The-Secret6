CREATE DATABASE IF NOT EXISTS enigma_db
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE enigma_db;

-- 按外键依赖顺序删除表
DROP TABLE IF EXISTS submission_clue;
DROP TABLE IF EXISTS game_submission;
DROP TABLE IF EXISTS room_stage_record;
DROP TABLE IF EXISTS player_clue;
DROP TABLE IF EXISTS game_result;
DROP TABLE IF EXISTS room_player;
DROP TABLE IF EXISTS game_room;
DROP TABLE IF EXISTS matchmaking_ticket;
DROP TABLE IF EXISTS script_answer;
DROP TABLE IF EXISTS clue_relation;
DROP TABLE IF EXISTS script_clue;
DROP TABLE IF EXISTS script_stage;
DROP TABLE IF EXISTS script_character;
DROP TABLE IF EXISTS game_script;
DROP TABLE IF EXISTS sys_user;

-- 用户账号表
CREATE TABLE sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(64) NOT NULL COMMENT '登录用户名',
    password VARCHAR(128) NOT NULL COMMENT 'BCrypt加密后的密码',
    nickname VARCHAR(64) NOT NULL COMMENT '用户昵称',
    avatar VARCHAR(256) DEFAULT NULL COMMENT '头像地址',
    bio VARCHAR(512) DEFAULT NULL COMMENT '个人简介',
    role VARCHAR(16) NOT NULL DEFAULT 'PLAYER'
        COMMENT '系统角色：PLAYER普通玩家，ADMIN管理员',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE'
        COMMENT '账号状态：ACTIVE正常，DISABLED禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='用户账号表';

-- 推理副本表
CREATE TABLE game_script (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '副本ID',
    title VARCHAR(128) NOT NULL COMMENT '副本名称',
    description TEXT COMMENT '副本简介',
    story TEXT COMMENT '案件背景',
    difficulty VARCHAR(16) NOT NULL DEFAULT 'MEDIUM'
        COMMENT '难度：EASY简单，MEDIUM中等，HARD困难',
    min_players INT NOT NULL DEFAULT 4 COMMENT '最少玩家人数',
    max_players INT NOT NULL DEFAULT 6 COMMENT '最多玩家人数',
    required_players INT NOT NULL DEFAULT 4 COMMENT '开局所需人数',
    estimated_minutes INT NOT NULL DEFAULT 60 COMMENT '预计时长，单位：分钟',
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT'
        COMMENT '副本状态：DRAFT草稿，PUBLISHED已发布，OFFLINE已下架',
    cover_image VARCHAR(256) DEFAULT NULL COMMENT '副本封面地址',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_script_status (status)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='推理副本表';

-- 副本人物角色表
CREATE TABLE script_character (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色配置ID',
    script_id BIGINT NOT NULL COMMENT '所属副本ID',
    character_name VARCHAR(64) NOT NULL COMMENT '人物名称',
    description TEXT COMMENT '人物背景和描述',
    character_goal TEXT COMMENT '人物目标或提示',
    is_hidden TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '是否为隐秘角色：0否，1是',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '人物显示顺序',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_character_script (script_id),
    CONSTRAINT fk_character_script
        FOREIGN KEY (script_id) REFERENCES game_script(id)
        ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='副本人物角色表';

-- 副本阶段表
CREATE TABLE script_stage (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '阶段ID',
    script_id BIGINT NOT NULL COMMENT '所属副本ID',
    stage_name VARCHAR(64) NOT NULL COMMENT '阶段名称',
    stage_description TEXT COMMENT '阶段说明',
    stage_order INT NOT NULL COMMENT '阶段顺序',
    unlock_rule TEXT COMMENT '阶段解锁规则JSON',
    timeout_minutes INT DEFAULT NULL COMMENT '阶段最长等待时间，单位：分钟',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stage_order (script_id, stage_order),
    INDEX idx_stage_script (script_id),
    CONSTRAINT fk_stage_script
        FOREIGN KEY (script_id) REFERENCES game_script(id)
        ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='副本阶段表';

-- 副本线索表
CREATE TABLE script_clue (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '线索ID',
    script_id BIGINT NOT NULL COMMENT '所属副本ID',
    stage_id BIGINT DEFAULT NULL COMMENT '所属阶段ID',
    title VARCHAR(128) NOT NULL COMMENT '线索标题',
    content TEXT NOT NULL COMMENT '线索内容',
    visibility_type VARCHAR(16) NOT NULL DEFAULT 'PUBLIC'
        COMMENT '可见范围：PUBLIC公开，CHARACTER指定人物可见，PLAYER指定玩家可见，HIDDEN隐藏',
    visible_character_id BIGINT DEFAULT NULL COMMENT '指定可见的人物角色ID',
    event_time VARCHAR(64) DEFAULT NULL COMMENT '线索对应的事件时间',
    location VARCHAR(128) DEFAULT NULL COMMENT '线索对应的地点',
    credibility INT NOT NULL DEFAULT 5 COMMENT '可信度，通常为1到10',
    is_key_clue TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否关键线索：0否，1是',
    is_misleading TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否误导线索：0否，1是',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '线索显示顺序',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_clue_script (script_id),
    INDEX idx_clue_stage (stage_id),
    INDEX idx_clue_character (visible_character_id),
    CONSTRAINT fk_clue_script
        FOREIGN KEY (script_id) REFERENCES game_script(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_clue_stage
        FOREIGN KEY (stage_id) REFERENCES script_stage(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_clue_character
        FOREIGN KEY (visible_character_id) REFERENCES script_character(id)
        ON DELETE SET NULL
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='副本线索表';

-- 线索与答案之间的证据关系表
CREATE TABLE clue_relation (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '证据关系ID',
    script_id BIGINT NOT NULL COMMENT '所属副本ID',
    clue_id BIGINT NOT NULL COMMENT '关联线索ID',
    answer_field VARCHAR(32) NOT NULL
        COMMENT '答案字段：CHARACTER人物，TIME时间，LOCATION地点，ACTION行为，KEY关键线索',
    relation_type VARCHAR(32) NOT NULL
        COMMENT '关系类型：SUPPORT支持，CONTRADICT矛盾，INDIRECT间接，INSUFFICIENT证据不足，KEY关键，MISLEADING误导',
    score INT NOT NULL DEFAULT 0 COMMENT '该证据关系对应的得分或扣分',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_clue_relation
        (script_id, clue_id, answer_field, relation_type),
    INDEX idx_relation_script (script_id),
    INDEX idx_relation_clue (clue_id),
    CONSTRAINT fk_relation_script
        FOREIGN KEY (script_id) REFERENCES game_script(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_relation_clue
        FOREIGN KEY (clue_id) REFERENCES script_clue(id)
        ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='线索证据关系表';

-- 副本标准答案表
CREATE TABLE script_answer (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '标准答案ID',
    script_id BIGINT NOT NULL COMMENT '所属副本ID',
    character_answer VARCHAR(128) NOT NULL COMMENT '人物标准答案',
    time_answer VARCHAR(128) NOT NULL COMMENT '时间标准答案',
    location_answer VARCHAR(128) NOT NULL COMMENT '地点标准答案',
    action_answer VARCHAR(256) NOT NULL COMMENT '行为或作案方式标准答案',
    scoring_rule_json TEXT COMMENT '评分规则JSON',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_answer_script (script_id),
    CONSTRAINT fk_answer_script
        FOREIGN KEY (script_id) REFERENCES game_script(id)
        ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='副本标准答案和评分规则表';

-- 玩家匹配记录表
CREATE TABLE matchmaking_ticket (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '匹配记录ID',
    user_id BIGINT NOT NULL COMMENT '玩家用户ID',
    script_id BIGINT NOT NULL COMMENT '匹配的副本ID',
    status VARCHAR(16) NOT NULL DEFAULT 'WAITING'
        COMMENT '匹配状态：WAITING等待，MATCHED成功，CANCELLED取消，EXPIRED过期',
    matched_room_id BIGINT DEFAULT NULL COMMENT '匹配成功后创建的房间ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入匹配时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '匹配状态更新时间',
    PRIMARY KEY (id),
    INDEX idx_match_status (status),
    INDEX idx_match_script_status (script_id, status),
    INDEX idx_match_user_script (user_id, script_id),
    CONSTRAINT fk_match_user
        FOREIGN KEY (user_id) REFERENCES sys_user(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_match_script
        FOREIGN KEY (script_id) REFERENCES game_script(id)
        ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='玩家副本匹配记录表';

-- 游戏房间表
CREATE TABLE game_room (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '房间ID',
    script_id BIGINT NOT NULL COMMENT '副本ID',
    current_stage_id BIGINT DEFAULT NULL COMMENT '当前阶段ID',
    status VARCHAR(16) NOT NULL DEFAULT 'WAITING'
        COMMENT '房间状态：WAITING等待，IN_PROGRESS进行中，FINISHED结束，ABANDONED中止',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '房间创建时间',
    start_time DATETIME DEFAULT NULL COMMENT '游戏开始时间',
    finish_time DATETIME DEFAULT NULL COMMENT '游戏结束时间',
    PRIMARY KEY (id),
    INDEX idx_room_script (script_id),
    INDEX idx_room_status (status),
    CONSTRAINT fk_room_script
        FOREIGN KEY (script_id) REFERENCES game_script(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_room_stage
        FOREIGN KEY (current_stage_id) REFERENCES script_stage(id)
        ON DELETE SET NULL
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='游戏房间表';

-- 房间玩家及人物分配表
CREATE TABLE room_player (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '房间玩家记录ID',
    room_id BIGINT NOT NULL COMMENT '房间ID',
    user_id BIGINT NOT NULL COMMENT '玩家用户ID',
    character_id BIGINT NOT NULL COMMENT '系统随机分配的人物角色ID',
    join_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入房间时间',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE'
        COMMENT '玩家状态：ACTIVE在房间，LEFT已离开',
    PRIMARY KEY (id),
    UNIQUE KEY uk_room_user (room_id, user_id),
    INDEX idx_room_player_user (user_id),
    CONSTRAINT fk_room_player_room
        FOREIGN KEY (room_id) REFERENCES game_room(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_room_player_user
        FOREIGN KEY (user_id) REFERENCES sys_user(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_room_player_character
        FOREIGN KEY (character_id) REFERENCES script_character(id)
        ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='房间玩家及人物分配表';

-- 玩家可查看线索记录表
CREATE TABLE player_clue (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '玩家线索记录ID',
    room_id BIGINT NOT NULL COMMENT '房间ID',
    user_id BIGINT NOT NULL COMMENT '玩家用户ID',
    clue_id BIGINT NOT NULL COMMENT '线索ID',
    is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已查看：0否，1是',
    acquire_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '获得线索时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_room_user_clue (room_id, user_id, clue_id),
    INDEX idx_player_clue_user (user_id),
    CONSTRAINT fk_player_clue_room
        FOREIGN KEY (room_id) REFERENCES game_room(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_player_clue_user
        FOREIGN KEY (user_id) REFERENCES sys_user(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_player_clue_clue
        FOREIGN KEY (clue_id) REFERENCES script_clue(id)
        ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='玩家可查看线索记录表';

-- 房间阶段解锁记录表
CREATE TABLE room_stage_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '阶段记录ID',
    room_id BIGINT NOT NULL COMMENT '房间ID',
    stage_id BIGINT NOT NULL COMMENT '副本阶段ID',
    unlock_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阶段解锁时间',
    unlock_reason VARCHAR(256) DEFAULT NULL COMMENT '阶段解锁原因',
    is_open TINYINT(1) NOT NULL DEFAULT 1 COMMENT '阶段是否开放：0否，1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_room_stage (room_id, stage_id),
    CONSTRAINT fk_stage_record_room
        FOREIGN KEY (room_id) REFERENCES game_room(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_stage_record_stage
        FOREIGN KEY (stage_id) REFERENCES script_stage(id)
        ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='房间阶段解锁记录表';

-- 玩家推理答案提交表
CREATE TABLE game_submission (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '推理提交ID',
    room_id BIGINT NOT NULL COMMENT '房间ID',
    user_id BIGINT NOT NULL COMMENT '提交玩家ID',
    character_answer VARCHAR(128) DEFAULT NULL COMMENT '玩家提交的人物答案',
    time_answer VARCHAR(128) DEFAULT NULL COMMENT '玩家提交的时间答案',
    location_answer VARCHAR(128) DEFAULT NULL COMMENT '玩家提交的地点答案',
    action_answer VARCHAR(256) DEFAULT NULL COMMENT '玩家提交的行为答案',
    explanation TEXT COMMENT '玩家推理说明',
    submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    score INT DEFAULT NULL COMMENT '本次得分',
    score_detail TEXT COMMENT '得分明细JSON',
    submission_order INT NOT NULL DEFAULT 1 COMMENT '提交次数或提交顺序',
    PRIMARY KEY (id),
    INDEX idx_submission_room_user (room_id, user_id),
    CONSTRAINT fk_submission_room
        FOREIGN KEY (room_id) REFERENCES game_room(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_submission_user
        FOREIGN KEY (user_id) REFERENCES sys_user(id)
        ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='玩家推理答案提交表';

-- 推理提交所引用的线索表
CREATE TABLE submission_clue (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '引用记录ID',
    submission_id BIGINT NOT NULL COMMENT '推理提交ID',
    clue_id BIGINT NOT NULL COMMENT '引用的线索ID',
    relation_type VARCHAR(32) DEFAULT NULL COMMENT '玩家选择的证据关系类型',
    PRIMARY KEY (id),
    UNIQUE KEY uk_submission_clue (submission_id, clue_id),
    INDEX idx_submission_clue_clue (clue_id),
    CONSTRAINT fk_submission_clue_submission
        FOREIGN KEY (submission_id) REFERENCES game_submission(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_submission_clue_clue
        FOREIGN KEY (clue_id) REFERENCES script_clue(id)
        ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='推理提交引用线索表';

-- 游戏结算结果表
CREATE TABLE game_result (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '游戏结算记录ID',
    room_id BIGINT NOT NULL COMMENT '房间ID',
    user_id BIGINT NOT NULL COMMENT '玩家用户ID',
    character_id BIGINT DEFAULT NULL COMMENT '玩家最终人物角色ID',
    final_answer VARCHAR(512) DEFAULT NULL COMMENT '玩家最终答案摘要',
    score INT NOT NULL DEFAULT 0 COMMENT '最终得分',
    score_detail TEXT COMMENT '评分明细JSON',
    result VARCHAR(32) DEFAULT NULL
        COMMENT '结算结果：SUCCESS成功，PARTIAL部分正确，FAIL失败',
    hidden_goal TINYINT(1) DEFAULT NULL COMMENT '隐秘角色目标是否达成',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '结算时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_result_room_user (room_id, user_id),
    INDEX idx_result_user (user_id),
    INDEX idx_result_room (room_id),
    CONSTRAINT fk_result_room
        FOREIGN KEY (room_id) REFERENCES game_room(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_result_user
        FOREIGN KEY (user_id) REFERENCES sys_user(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_result_character
        FOREIGN KEY (character_id) REFERENCES script_character(id)
        ON DELETE SET NULL
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='游戏结算结果表';