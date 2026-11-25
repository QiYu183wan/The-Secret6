USE enigma_db;

SET NAMES utf8mb4;

-- 清空示例数据；先删子表数据，再删父表数据
DELETE FROM submission_clue;
DELETE FROM game_submission;
DELETE FROM room_stage_record;
DELETE FROM player_clue;
DELETE FROM game_result;
DELETE FROM room_player;
DELETE FROM game_room;
DELETE FROM matchmaking_ticket;
DELETE FROM script_answer;
DELETE FROM clue_relation;
DELETE FROM script_clue;
DELETE FROM script_stage;
DELETE FROM script_character;
DELETE FROM game_script;

-- 创建示例副本
INSERT INTO game_script (
    title,
    description,
    story,
    difficulty,
    min_players,
    max_players,
    required_players,
    estimated_minutes,
    status
) VALUES (
    '午夜档案',
    '大学档案馆的一份重要档案在闭馆后失踪。四位玩家各自掌握部分线索，需要通过证据推理找出真相。',
    '2024年11月15日凌晨，江城市大学档案馆的一份机密研究档案失踪。门禁记录、巡逻记录和档案交接信息之间存在矛盾。',
    'MEDIUM',
    4,
    4,
    4,
    90,
    'PUBLISHED'
);

SET @script_id = (
    SELECT id
    FROM game_script
    WHERE title = '午夜档案'
    ORDER BY id DESC
    LIMIT 1
);

-- 创建副本人物
INSERT INTO script_character (
    script_id,
    character_name,
    description,
    character_goal,
    is_hidden,
    sort_order
) VALUES
(
    @script_id,
    '档案调查员',
    '负责调查档案失踪案，熟悉门禁系统和档案分类。',
    '分析门禁和档案资料，找出真相。',
    0,
    1
),
(
    @script_id,
    '夜班保安',
    '当晚值班保安，负责巡逻和安保监控。',
    '还原巡逻情况和目击信息。',
    0,
    2
),
(
    @script_id,
    '档案管理员',
    '负责档案的日常管理和交接工作。',
    '还原档案交接流程和工作日志中的异常。',
    0,
    3
),
(
    @script_id,
    '外部研究员',
    '曾申请借阅档案的外部研究人员。',
    '根据掌握的信息参与案件推理。',
    0,
    4
);

-- 查询人物ID，避免依赖自增ID固定为1、2、3、4
SET @char_investigator = (
    SELECT id FROM script_character
    WHERE script_id = @script_id AND character_name = '档案调查员'
    LIMIT 1
);

SET @char_guard = (
    SELECT id FROM script_character
    WHERE script_id = @script_id AND character_name = '夜班保安'
    LIMIT 1
);

SET @char_manager = (
    SELECT id FROM script_character
    WHERE script_id = @script_id AND character_name = '档案管理员'
    LIMIT 1
);

SET @char_researcher = (
    SELECT id FROM script_character
    WHERE script_id = @script_id AND character_name = '外部研究员'
    LIMIT 1
);

-- 创建副本阶段
INSERT INTO script_stage (
    script_id,
    stage_name,
    stage_description,
    stage_order,
    unlock_rule,
    timeout_minutes
) VALUES
(
    @script_id,
    '线索调查',
    '玩家查看公开线索和自己人物对应的私人线索。',
    1,
    '{"type":"START"}',
    30
),
(
    @script_id,
    '答案提交',
    '玩家提交结构化答案并引用支持推理的线索。',
    2,
    '{"type":"MANUAL"}',
    30
),
(
    @script_id,
    '结算复盘',
    '系统按照标准答案和证据关系计算得分并展示复盘。',
    3,
    '{"type":"AUTO_RESULT"}',
    10
);

SET @stage_investigation = (
    SELECT id FROM script_stage
    WHERE script_id = @script_id AND stage_order = 1
    LIMIT 1
);

SET @stage_submission = (
    SELECT id FROM script_stage
    WHERE script_id = @script_id AND stage_order = 2
    LIMIT 1
);

-- 创建副本线索
INSERT INTO script_clue (
    script_id,
    stage_id,
    title,
    content,
    visibility_type,
    visible_character_id,
    event_time,
    location,
    credibility,
    is_key_clue,
    is_misleading,
    sort_order
) VALUES
(
    @script_id,
    @stage_investigation,
    '档案室门禁记录',
    '11月15日凌晨00:42，档案室门禁显示临时访客卡#7进入。登记资料显示该卡由外部研究员使用。',
    'PUBLIC',
    NULL,
    '2024-11-15 00:42',
    '档案室门口',
    9,
    1,
    0,
    1
),
(
    @script_id,
    @stage_investigation,
    '夜班巡逻记录',
    '夜班保安记录显示，01:35巡逻时发现档案室门虚掩。',
    'PUBLIC',
    NULL,
    '2024-11-15 01:35',
    '图书馆三楼',
    7,
    0,
    0,
    2
),
(
    @script_id,
    @stage_investigation,
    '档案交接单',
    '11月14日17:00，档案管理员将研究档案归档至档案室B区-03-12。',
    'PUBLIC',
    NULL,
    '2024-11-14 17:00',
    '档案室B区',
    8,
    1,
    0,
    3
),
(
    @script_id,
    @stage_investigation,
    '监控录像片段',
    '凌晨00:38至00:45，档案室走廊监控出现黑屏。',
    'CHARACTER',
    @char_investigator,
    '2024-11-15 00:38-00:45',
    '档案室走廊',
    9,
    1,
    0,
    4
),
(
    @script_id,
    @stage_investigation,
    '档案柜照片',
    '档案柜B区-03-12附近发现轻微撬动痕迹。',
    'CHARACTER',
    @char_investigator,
    NULL,
    '档案室B区',
    7,
    0,
    0,
    5
),
(
    @script_id,
    @stage_investigation,
    '走廊目击记录',
    '凌晨00:45左右，有一名穿深色外套的人从三楼楼梯口匆忙离开，但目击者无法确认其身份。',
    'CHARACTER',
    @char_guard,
    '2024-11-15 00:45',
    '三楼楼梯口',
    6,
    0,
    1,
    6
),
(
    @script_id,
    @stage_investigation,
    '值班室登记日志',
    '登记日志显示，外部研究员于11月14日18:00进入图书馆，未记录离开时间。',
    'CHARACTER',
    @char_guard,
    '2024-11-14 18:00',
    '值班室',
    8,
    1,
    0,
    7
),
(
    @script_id,
    @stage_investigation,
    '管理员工作日志',
    '档案管理员记录自己于21:00离开，并在次日上午发现档案柜异常。',
    'CHARACTER',
    @char_manager,
    '2024-11-15 08:00',
    '档案室',
    7,
    0,
    0,
    8
),
(
    @script_id,
    @stage_investigation,
    '借阅申请记录',
    '外部研究员曾申请临时借阅档案，但由于档案属于机密级别，申请被拒绝。',
    'CHARACTER',
    @char_manager,
    '2024-11-14 15:30',
    '档案管理员办公室',
    8,
    1,
    0,
    9
),
(
    @script_id,
    @stage_submission,
    '临时访客卡申请记录',
    '临时访客卡#7的签领记录存在疑点，签名与登记资料不完全一致。',
    'CHARACTER',
    @char_researcher,
    '2024-11-14 16:00',
    '行政办公室',
    8,
    1,
    0,
    10
),
(
    @script_id,
    @stage_submission,
    '伪造的电话记录',
    '一张声称调查员与外部研究员在案发时通话的截图，号码信息与登记资料不符。',
    'PUBLIC',
    NULL,
    '2024-11-15 00:30',
    '电话记录',
    3,
    0,
    1,
    11
);

-- 查询线索ID，避免依赖固定自增ID
SET @clue_door = (
    SELECT id FROM script_clue
    WHERE script_id = @script_id AND title = '档案室门禁记录'
    LIMIT 1
);

SET @clue_patrol = (
    SELECT id FROM script_clue
    WHERE script_id = @script_id AND title = '夜班巡逻记录'
    LIMIT 1
);

SET @clue_transfer = (
    SELECT id FROM script_clue
    WHERE script_id = @script_id AND title = '档案交接单'
    LIMIT 1
);

SET @clue_video = (
    SELECT id FROM script_clue
    WHERE script_id = @script_id AND title = '监控录像片段'
    LIMIT 1
);

SET @clue_cabinet = (
    SELECT id FROM script_clue
    WHERE script_id = @script_id AND title = '档案柜照片'
    LIMIT 1
);

SET @clue_witness = (
    SELECT id FROM script_clue
    WHERE script_id = @script_id AND title = '走廊目击记录'
    LIMIT 1
);

SET @clue_log = (
    SELECT id FROM script_clue
    WHERE script_id = @script_id AND title = '值班室登记日志'
    LIMIT 1
);

SET @clue_application = (
    SELECT id FROM script_clue
    WHERE script_id = @script_id AND title = '借阅申请记录'
    LIMIT 1
);

SET @clue_card = (
    SELECT id FROM script_clue
    WHERE script_id = @script_id AND title = '临时访客卡申请记录'
    LIMIT 1
);

SET @clue_phone = (
    SELECT id FROM script_clue
    WHERE script_id = @script_id AND title = '伪造的电话记录'
    LIMIT 1
);

-- 创建副本标准答案和评分规则
INSERT INTO script_answer (
    script_id,
    character_answer,
    time_answer,
    location_answer,
    action_answer,
    scoring_rule_json
) VALUES (
    @script_id,
    '外部研究员',
    '11月15日凌晨00:42',
    '档案室B区-03-12',
    '使用临时访客卡进入档案室并带走档案',
    '{"characterCorrect":30,"timeCorrect":20,"locationCorrect":15,"actionCorrect":20,"supportClueMax":10,"contradictPerPenalty":5,"missingKeyPenalty":5}'
);

-- 配置线索与答案字段的关系
INSERT INTO clue_relation (
    script_id,
    clue_id,
    answer_field,
    relation_type,
    score
) VALUES
(@script_id, @clue_door, 'CHARACTER', 'SUPPORT', 5),
(@script_id, @clue_door, 'TIME', 'SUPPORT', 5),
(@script_id, @clue_door, 'KEY', 'KEY', 0),

(@script_id, @clue_video, 'ACTION', 'SUPPORT', 5),
(@script_id, @clue_video, 'KEY', 'KEY', 0),

(@script_id, @clue_log, 'CHARACTER', 'SUPPORT', 5),
(@script_id, @clue_log, 'KEY', 'KEY', 0),

(@script_id, @clue_application, 'CHARACTER', 'INDIRECT', 3),
(@script_id, @clue_card, 'CHARACTER', 'SUPPORT', 5),
(@script_id, @clue_card, 'KEY', 'KEY', 0),

(@script_id, @clue_patrol, 'TIME', 'INDIRECT', 2),
(@script_id, @clue_cabinet, 'LOCATION', 'INDIRECT', 3),
(@script_id, @clue_transfer, 'LOCATION', 'SUPPORT', 3),

(@script_id, @clue_witness, 'CHARACTER', 'MISLEADING', 0),
(@script_id, @clue_phone, 'CHARACTER', 'MISLEADING', 0);

-- ========== 单人副本：消失的密钥 ==========
INSERT INTO game_script (
    title, description, story, difficulty,
    min_players, max_players, required_players,
    estimated_minutes, status
) VALUES (
    '消失的密钥（单人）',
    '单人推理副本：深夜档案馆中，一份密钥在监控盲区内消失，所有线索都指向内部人员。',
    '12月3日深夜，私人档案馆馆长陈默发现保险柜中的密钥U盘失踪。门窗完好，保险柜没有被撬动。你是受委托赶到现场的唯一调查员，需要在警方到达前还原真相。',
    'EASY', 1, 1, 1, 15, 'PUBLISHED'
);

SET @solo_script = LAST_INSERT_ID();

INSERT INTO script_character (
    script_id, character_name, description, character_goal, is_hidden, sort_order
) VALUES (
    @solo_script,
    '调查员',
    '受托在警方到达前勘查现场的自由调查员，可以接触公开线索和一项内部资料。',
    '根据门禁、监控、保险柜和系统日志，还原密钥失窃的经过。',
    0, 1
);

SET @solo_char = LAST_INSERT_ID();

INSERT INTO script_stage (
    script_id, stage_name, stage_description, stage_order, unlock_rule, timeout_minutes
) VALUES (
    @solo_script,
    '现场勘查',
    '检查门禁、监控、保险柜与密钥系统，锁定嫌疑人和作案过程。',
    1, NULL, NULL
);

SET @solo_stage = LAST_INSERT_ID();

INSERT INTO script_clue (
    script_id, stage_id, title, content, visibility_type, visible_character_id,
    event_time, location, credibility, is_key_clue, is_misleading, sort_order
) VALUES
(@solo_script, @solo_stage, '门禁刷卡记录',
 '23:47，馆长办公室被一张备用门卡刷开。备用门卡当晚由馆长助理周岚保管，她声称整晚都在自己的工位加班。',
 'PUBLIC', NULL, '12月3日 23:47', '馆长办公室门口', 9, 1, 0, 1),
(@solo_script, @solo_stage, '监控维护记录',
 '23:40 至 23:55，三楼走廊监控进入维护模式，只有内部人员知道维护口令。维护日志显示操作账号为助理周岚。',
 'PUBLIC', NULL, '12月3日 23:40-23:55', '三楼走廊', 8, 0, 0, 2),
(@solo_script, @solo_stage, '保险柜勘查',
 '保险柜没有被撬动，使用正确密码开启。密码仅馆长陈默和助理周岚知道。',
 'PUBLIC', NULL, NULL, '馆长办公室', 9, 0, 0, 3),
(@solo_script, @solo_stage, '清洁工证词',
 '清洁工称 23:52 左右看到周岚从馆长办公室方向出来，手里拿着一个牛皮纸袋。',
 'PUBLIC', NULL, '12月3日 23:52', '馆长办公室走廊', 7, 0, 0, 4),
(@solo_script, @solo_stage, '密钥系统日志',
 '23:52，密钥管理系统使用周岚的账号登录，并导出名为 KEYS-2024 的密钥文件。',
 'PUBLIC', NULL, '12月3日 23:52', '密钥管理系统', 10, 1, 0, 5),
(@solo_script, @solo_stage, '夜间推销电话',
 '座机在 22:30 接到一个陌生来电。运营商记录显示该号码属于自动推销平台，通话仅 12 秒。',
 'PUBLIC', NULL, '12月3日 22:30', '馆长办公室座机', 3, 0, 1, 6),
(@solo_script, @solo_stage, '加班申请',
 '周岚提交的加班申请为 20:00 至次日 02:00，理由写着「整理档案」，但档案室签到表没有她的记录。',
 'CHARACTER', @solo_char, '12月3日 20:00-次日02:00', '助理工位', 6, 0, 0, 7);

SET @solo_door = (SELECT id FROM script_clue WHERE script_id = @solo_script AND title = '门禁刷卡记录' LIMIT 1);
SET @solo_camera = (SELECT id FROM script_clue WHERE script_id = @solo_script AND title = '监控维护记录' LIMIT 1);
SET @solo_safe = (SELECT id FROM script_clue WHERE script_id = @solo_script AND title = '保险柜勘查' LIMIT 1);
SET @solo_cleaner = (SELECT id FROM script_clue WHERE script_id = @solo_script AND title = '清洁工证词' LIMIT 1);
SET @solo_log = (SELECT id FROM script_clue WHERE script_id = @solo_script AND title = '密钥系统日志' LIMIT 1);
SET @solo_call = (SELECT id FROM script_clue WHERE script_id = @solo_script AND title = '夜间推销电话' LIMIT 1);
SET @solo_overtime = (SELECT id FROM script_clue WHERE script_id = @solo_script AND title = '加班申请' LIMIT 1);

INSERT INTO script_answer (
    script_id, character_answer, time_answer, location_answer, action_answer, scoring_rule_json
) VALUES (
    @solo_script,
    '周岚（馆长助理）',
    '12月3日23:47',
    '馆长办公室-保险柜前',
    '使用备用门卡进入馆长办公室，用密码打开保险柜并带走密钥文件',
    '{"characterCorrect":30,"timeCorrect":20,"locationCorrect":15,"actionCorrect":20,"supportClueMax":10,"contradictPerPenalty":5,"missingKeyPenalty":5,"timeConflictPenalty":10,"keyBonus":10}'
);

INSERT INTO clue_relation (script_id, clue_id, answer_field, relation_type, score) VALUES
(@solo_script, @solo_door, 'CHARACTER', 'SUPPORT', 5),
(@solo_script, @solo_door, 'TIME', 'SUPPORT', 5),
(@solo_script, @solo_door, 'LOCATION', 'SUPPORT', 5),
(@solo_script, @solo_door, 'KEY', 'KEY', 0),

(@solo_script, @solo_camera, 'CHARACTER', 'SUPPORT', 5),
(@solo_script, @solo_camera, 'ACTION', 'SUPPORT', 5),

(@solo_script, @solo_safe, 'CHARACTER', 'SUPPORT', 5),
(@solo_script, @solo_safe, 'ACTION', 'SUPPORT', 5),
(@solo_script, @solo_safe, 'LOCATION', 'SUPPORT', 5),

(@solo_script, @solo_cleaner, 'CHARACTER', 'SUPPORT', 5),
(@solo_script, @solo_cleaner, 'TIME', 'SUPPORT', 5),
(@solo_script, @solo_cleaner, 'LOCATION', 'SUPPORT', 5),
(@solo_script, @solo_cleaner, 'ACTION', 'SUPPORT', 5),

(@solo_script, @solo_log, 'CHARACTER', 'SUPPORT', 5),
(@solo_script, @solo_log, 'TIME', 'SUPPORT', 5),
(@solo_script, @solo_log, 'ACTION', 'SUPPORT', 5),
(@solo_script, @solo_log, 'KEY', 'KEY', 0),

(@solo_script, @solo_call, 'ACTION', 'MISLEADING', 0),

(@solo_script, @solo_overtime, 'CHARACTER', 'SUPPORT', 5),
(@solo_script, @solo_overtime, 'ACTION', 'SUPPORT', 5);

-- ========== 双人副本：双生证词 ==========
INSERT INTO game_script (
    title, description, story, difficulty,
    min_players, max_players, required_players,
    estimated_minutes, status
) VALUES (
    '双生证词（双人）',
    '双人推理副本：一幅画作在闭馆后被人调包，两名调查员各自掌握一半现场信息。',
    '12月8日 21:30，私人美术馆闭馆清点时，工作人员发现展出的《双生》已被换成复制品。门窗完好，报警器没有触发。你是赶到现场的两名调查员之一，需要在真迹被运走前还原调包过程。',
    'MEDIUM', 2, 2, 2, 25, 'PUBLISHED'
);

SET @duo_script = LAST_INSERT_ID();

INSERT INTO script_character (
    script_id, character_name, description, character_goal, is_hidden, sort_order
) VALUES
(
    @duo_script,
    '现场调查员',
    '负责检查展厅、储藏室和画框痕迹，掌握门锁相关的现场细节。',
    '与搭档交换信息，还原画作被调包的具体过程。',
    0, 1
),
(
    @duo_script,
    '监控分析员',
    '负责调取监控、门禁和货梯记录，掌握时间线上的关键缺口。',
    '与搭档核对时间线，锁定调包者。',
    0, 2
);

SET @duo_scene = (SELECT id FROM script_character WHERE script_id = @duo_script AND character_name = '现场调查员' LIMIT 1);
SET @duo_monitor = (SELECT id FROM script_character WHERE script_id = @duo_script AND character_name = '监控分析员' LIMIT 1);

INSERT INTO script_stage (
    script_id, stage_name, stage_description, stage_order, unlock_rule, timeout_minutes
) VALUES (
    @duo_script,
    '联合取证',
    '两名调查员分别检查现场与监控记录，交换线索后提交共同推理。',
    1, NULL, NULL
);

SET @duo_stage = LAST_INSERT_ID();

INSERT INTO script_clue (
    script_id, stage_id, title, content, visibility_type, visible_character_id,
    event_time, location, credibility, is_key_clue, is_misleading, sort_order
) VALUES
(@duo_script, @duo_stage, '展厅监控',
 '21:12，苏芮进入二层储藏室，三分钟后抱着一个长画筒离开。',
 'PUBLIC', NULL, '12月8日 21:12', '二层储藏室', 9, 1, 0, 1),
(@duo_script, @duo_stage, '货梯门禁记录',
 '21:15，货梯被苏芮的工牌启动，从二层直达地下车库，停留了四分钟。',
 'PUBLIC', NULL, '12月8日 21:15', '二层货梯', 9, 0, 0, 2),
(@duo_script, @duo_stage, '画框检测',
 '展厅中的《双生》颜料层为现代打印材料，真迹在当晚已被替换。',
 'PUBLIC', NULL, NULL, '一层展厅', 10, 0, 0, 3),
(@duo_script, @duo_stage, '复制品订单',
 '一周前，有人使用苏芮的账号下单定制同尺寸画布，收货地址填的是美术馆后门。',
 'PUBLIC', NULL, '12月1日', '网络订单系统', 8, 1, 0, 4),
(@duo_script, @duo_stage, '停车场目击',
 '地下车库保安称，21:20 左右看到苏芮把一个长条纸箱放进后备箱。',
 'PUBLIC', NULL, '12月8日 21:20', '地下车库', 7, 0, 0, 5),
(@duo_script, @duo_stage, '匿名举报邮件',
 '一封匿名邮件称「外部修复师」会在当晚取画，但发件 IP 来自一家公共网吧。',
 'PUBLIC', NULL, '12月8日 18:40', '邮件系统', 3, 0, 1, 6),
(@duo_script, @duo_stage, '储藏室门锁细痕',
 '锁芯有新鲜划痕，说明有人用备用钥匙反复试插。备用钥匙当晚由苏芮保管。',
 'CHARACTER', @duo_scene, NULL, '二层储藏室', 8, 0, 0, 7),
(@duo_script, @duo_stage, '监控时间缺口',
 '21:10 至 21:13 的监控缺失了 17 秒，删除操作使用的是苏芮的账号。',
 'CHARACTER', @duo_monitor, '12月8日 21:10-21:13', '监控后台', 9, 0, 0, 8);

SET @duo_cctv = (SELECT id FROM script_clue WHERE script_id = @duo_script AND title = '展厅监控' LIMIT 1);
SET @duo_lift = (SELECT id FROM script_clue WHERE script_id = @duo_script AND title = '货梯门禁记录' LIMIT 1);
SET @duo_frame = (SELECT id FROM script_clue WHERE script_id = @duo_script AND title = '画框检测' LIMIT 1);
SET @duo_order = (SELECT id FROM script_clue WHERE script_id = @duo_script AND title = '复制品订单' LIMIT 1);
SET @duo_garage = (SELECT id FROM script_clue WHERE script_id = @duo_script AND title = '停车场目击' LIMIT 1);
SET @duo_mail = (SELECT id FROM script_clue WHERE script_id = @duo_script AND title = '匿名举报邮件' LIMIT 1);
SET @duo_lock = (SELECT id FROM script_clue WHERE script_id = @duo_script AND title = '储藏室门锁细痕' LIMIT 1);
SET @duo_gap = (SELECT id FROM script_clue WHERE script_id = @duo_script AND title = '监控时间缺口' LIMIT 1);

INSERT INTO script_answer (
    script_id, character_answer, time_answer, location_answer, action_answer, scoring_rule_json
) VALUES (
    @duo_script,
    '苏芮（助理策展人）',
    '12月8日21:12',
    '二层储藏室',
    '用复制品调包真迹，并通过货梯将真迹运走',
    '{"characterCorrect":30,"timeCorrect":20,"locationCorrect":15,"actionCorrect":20,"supportClueMax":10,"contradictPerPenalty":5,"missingKeyPenalty":5,"timeConflictPenalty":10,"keyBonus":10}'
);

INSERT INTO clue_relation (script_id, clue_id, answer_field, relation_type, score) VALUES
(@duo_script, @duo_cctv, 'CHARACTER', 'SUPPORT', 5),
(@duo_script, @duo_cctv, 'TIME', 'SUPPORT', 5),
(@duo_script, @duo_cctv, 'LOCATION', 'SUPPORT', 5),
(@duo_script, @duo_cctv, 'ACTION', 'SUPPORT', 5),
(@duo_script, @duo_cctv, 'KEY', 'KEY', 0),

(@duo_script, @duo_lift, 'CHARACTER', 'SUPPORT', 5),
(@duo_script, @duo_lift, 'TIME', 'SUPPORT', 5),
(@duo_script, @duo_lift, 'ACTION', 'SUPPORT', 5),

(@duo_script, @duo_frame, 'ACTION', 'SUPPORT', 5),

(@duo_script, @duo_order, 'CHARACTER', 'SUPPORT', 5),
(@duo_script, @duo_order, 'ACTION', 'SUPPORT', 5),
(@duo_script, @duo_order, 'KEY', 'KEY', 0),

(@duo_script, @duo_garage, 'CHARACTER', 'SUPPORT', 5),
(@duo_script, @duo_garage, 'TIME', 'SUPPORT', 5),
(@duo_script, @duo_garage, 'ACTION', 'SUPPORT', 5),

(@duo_script, @duo_mail, 'ACTION', 'MISLEADING', 0),

(@duo_script, @duo_lock, 'CHARACTER', 'SUPPORT', 5),
(@duo_script, @duo_lock, 'ACTION', 'SUPPORT', 5),

(@duo_script, @duo_gap, 'CHARACTER', 'SUPPORT', 5),
(@duo_script, @duo_gap, 'TIME', 'SUPPORT', 5),
(@duo_script, @duo_gap, 'ACTION', 'SUPPORT', 5);