package com.enigma.service;

import com.enigma.common.BusinessException;
import com.enigma.dto.RequestDtos;
import com.enigma.dto.ViewDtos;
import com.enigma.entity.ClueRelation;
import com.enigma.entity.GameResult;
import com.enigma.entity.GameRoom;
import com.enigma.entity.GameScript;
import com.enigma.entity.GameSubmission;
import com.enigma.entity.PlayerClue;
import com.enigma.entity.RoomPlayer;
import com.enigma.entity.RoomStageRecord;
import com.enigma.entity.ScriptAnswer;
import com.enigma.entity.ScriptCharacter;
import com.enigma.entity.ScriptClue;
import com.enigma.entity.ScriptStage;
import com.enigma.entity.SubmissionClue;
import com.enigma.entity.SysUser;
import com.enigma.mapper.ClueRelationMapper;
import com.enigma.mapper.GameResultMapper;
import com.enigma.mapper.GameRoomMapper;
import com.enigma.mapper.GameScriptMapper;
import com.enigma.mapper.GameSubmissionMapper;
import com.enigma.mapper.PlayerClueMapper;
import com.enigma.mapper.RoomPlayerMapper;
import com.enigma.mapper.RoomStageRecordMapper;
import com.enigma.mapper.ScriptAnswerMapper;
import com.enigma.mapper.ScriptCharacterMapper;
import com.enigma.mapper.ScriptClueMapper;
import com.enigma.mapper.ScriptStageMapper;
import com.enigma.mapper.SubmissionClueMapper;
import com.enigma.mapper.SysUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final GameRoomMapper roomMapper;
    private final GameScriptMapper scriptMapper;
    private final RoomPlayerMapper roomPlayerMapper;
    private final ScriptCharacterMapper characterMapper;
    private final ScriptStageMapper stageMapper;
    private final ScriptClueMapper clueMapper;
    private final ClueRelationMapper relationMapper;
    private final ScriptAnswerMapper answerMapper;
    private final PlayerClueMapper playerClueMapper;
    private final GameSubmissionMapper submissionMapper;
    private final SubmissionClueMapper submissionClueMapper;
    private final GameResultMapper resultMapper;
    private final RoomStageRecordMapper stageRecordMapper;
    private final SysUserMapper userMapper;
    private final ClueAccessService clueAccess;
    private final ScoringService scoringService;
    private final ObjectMapper objectMapper;

    // ---------------- 房间基础信息 ----------------

    public ViewDtos.RoomInfo getRoomInfo(Long roomId, Long userId) {
        GameRoom room = requireRoom(roomId);
        RoomPlayer me = requireMember(roomId, userId);
        GameScript script = scriptMapper.selectById(room.getScriptId());
        List<RoomPlayer> members = roomPlayerMapper.selectByRoomId(roomId);
        Map<Long, SysUser> users = loadUsers(members);
        Map<Long, ScriptCharacter> characters = loadCharacters(room.getScriptId());
        List<ScriptStage> stages = loadStages(room.getScriptId());
        ScriptStage current = room.getCurrentStageId() == null ? null : stageMapper.selectById(room.getCurrentStageId());

        List<ViewDtos.RoomPlayerItem> players = members.stream()
                .map(member -> {
                    ScriptCharacter character = characters.get(member.getCharacterId());
                    SysUser user = users.get(member.getUserId());
                    return new ViewDtos.RoomPlayerItem(member.getUserId(),
                            user == null ? "未知玩家" : user.getNickname(),
                            member.getCharacterId(),
                            character == null ? "未知角色" : character.getCharacterName(),
                            character != null && Integer.valueOf(1).equals(character.getIsHidden()),
                            member.getJoinTime());
                }).toList();

        long submissionCount = submissionMapper.countByRoom(roomId);
        GameSubmission mine = latestSubmission(roomId, userId);

        List<String> characterNames = characterMapper.selectByScriptId(room.getScriptId())
                .stream().map(ScriptCharacter::getCharacterName).toList();

        return new ViewDtos.RoomInfo(room.getId(), room.getScriptId(), script == null ? "" : script.getTitle(),
                room.getStatus(), room.getCurrentStageId(),
                current == null ? null : current.getStageOrder(),
                current == null ? null : current.getStageName(),
                room.getCreateTime(), room.getStartTime(), room.getFinishTime(),
                players, characterNames, mine != null, submissionCount >= members.size(),
                mine == null ? null : mine.getScore());
    }

    public ViewDtos.MyCharacter getMyCharacter(Long roomId, Long userId) {
        requireRoom(roomId);
        RoomPlayer member = requireMember(roomId, userId);
        ScriptCharacter character = characterMapper.selectById(member.getCharacterId());
        if (character == null) {
            throw new BusinessException(404, "角色不存在");
        }
        return new ViewDtos.MyCharacter(character.getId(), character.getCharacterName(),
                character.getDescription(), character.getCharacterGoal(),
                Integer.valueOf(1).equals(character.getIsHidden()));
    }

    public ViewDtos.StageProgress getStageProgress(Long roomId, Long userId) {
        GameRoom room = requireRoom(roomId);
        requireMember(roomId, userId);
        List<ScriptStage> stages = loadStages(room.getScriptId());
        Map<Long, RoomStageRecord> records = stageRecordMapper.selectByRoom(roomId)
                .stream().collect(Collectors.toMap(RoomStageRecord::getStageId, Function.identity(), (a, b) -> a));
        int currentOrder = clueAccess.currentOrder(room, stages);
        List<ViewDtos.StageProgressItem> items = stages.stream().map(stage -> {
            RoomStageRecord record = records.get(stage.getId());
            boolean open = (record != null && Integer.valueOf(1).equals(record.getIsOpen()))
                    || stage.getStageOrder() <= currentOrder;
            return new ViewDtos.StageProgressItem(stage.getId(), stage.getStageName(),
                    stage.getStageDescription(), stage.getStageOrder(), open,
                    record == null ? null : record.getUnlockTime());
        }).toList();
        return new ViewDtos.StageProgress(room.getCurrentStageId(), currentOrder, items);
    }

    // ---------------- 线索 ----------------

    public List<ViewDtos.ClueItem> getClues(Long roomId, Long userId) {
        GameRoom room = requireRoom(roomId);
        RoomPlayer me = requireMember(roomId, userId);
        List<ScriptStage> stages = loadStages(room.getScriptId());
        Map<Long, Integer> stageOrders = clueAccess.stageOrderMap(stages);
        Map<Long, String> stageNames = clueAccess.stageNameMap(stages);
        int currentOrder = clueAccess.currentOrder(room, stages);
        List<ScriptClue> clues = clueMapper.selectByScriptId(room.getScriptId());
        Map<Long, PlayerClue> owned = playerClueMapper.selectByRoomAndUser(roomId, userId)
                .stream().collect(Collectors.toMap(PlayerClue::getClueId, Function.identity(), (a, b) -> a));

        List<ViewDtos.ClueItem> result = new ArrayList<>();
        for (ScriptClue clue : clues) {
            if (!clueAccess.isVisible(clue, me.getCharacterId(), stageOrders, currentOrder)) {
                continue;
            }
            if (!owned.containsKey(clue.getId())) {
                PlayerClue record = new PlayerClue();
                record.setRoomId(roomId);
                record.setUserId(userId);
                record.setClueId(clue.getId());
                record.setIsRead(0);
                playerClueMapper.insert(record);
            }
            result.add(clueAccess.toItem(clue, stageNames, true, true));
        }
        playerClueMapper.markAllRead(roomId, userId);
        return result;
    }

    public ViewDtos.EvidenceChain getEvidenceChain(Long roomId, Long userId) {
        GameRoom room = requireRoom(roomId);
        requireMember(roomId, userId);
        List<ViewDtos.ClueItem> visible = getClues(roomId, userId);
        List<ClueRelation> relations = relationMapper.selectByScriptId(room.getScriptId());
        Map<Long, List<ClueRelation>> relationsByClue = relations.stream()
                .collect(Collectors.groupingBy(ClueRelation::getClueId));

        GameSubmission submission = latestSubmission(roomId, userId);
        Set<Long> referenced = new LinkedHashSet<>();
        if (submission != null) {
            submissionClueMapper.selectBySubmissionId(submission.getId())
                    .forEach(ref -> referenced.add(ref.getClueId()));
        }

        List<ViewDtos.ClueItem> support = new ArrayList<>();
        List<ViewDtos.ClueItem> key = new ArrayList<>();
        List<ViewDtos.ClueItem> indirect = new ArrayList<>();
        List<ViewDtos.ClueItem> contradict = new ArrayList<>();
        List<ViewDtos.ClueItem> misleading = new ArrayList<>();
        List<ViewDtos.ClueItem> unreferenced = new ArrayList<>();

        for (ViewDtos.ClueItem item : visible) {
            boolean isReferenced = referenced.contains(item.id());
            if (!isReferenced) {
                unreferenced.add(item);
            }
            List<ClueRelation> clueRelations = relationsByClue.getOrDefault(item.id(), List.of());
            boolean isKey = Integer.valueOf(1).equals(item.isKeyClue());
            boolean isMisleading = Integer.valueOf(1).equals(item.isMisleading());
            for (ClueRelation relation : clueRelations) {
                switch (relation.getRelationType()) {
                    case "SUPPORT" -> support.add(item);
                    case "CONTRADICT" -> contradict.add(item);
                    case "INDIRECT" -> indirect.add(item);
                    case "KEY" -> isKey = true;
                    case "MISLEADING" -> isMisleading = true;
                    default -> {
                    }
                }
            }
            if (isKey) {
                key.add(item);
            }
            if (isMisleading) {
                misleading.add(item);
            }
        }

        List<String> hints = new ArrayList<>();
        if (submission == null) {
            hints.add("尚未提交推理结论，请先提交结构化答案");
        } else {
            boolean anySupport = support.stream().anyMatch(item -> referenced.contains(item.id()));
            if (!anySupport) {
                hints.add("尚未引用支持结论的线索");
            }
            boolean anyKey = key.stream().anyMatch(item -> referenced.contains(item.id()));
            if (!anyKey) {
                hints.add("尚未引用关键线索");
            }
            long missingKey = key.stream().filter(item -> !referenced.contains(item.id())).count();
            if (missingKey > 0) {
                hints.add("还有 " + missingKey + " 条关键线索未引用");
            }
            if (contradict.stream().anyMatch(item -> referenced.contains(item.id()))) {
                hints.add("引用了与结论矛盾的线索");
            }
            if (misleading.stream().anyMatch(item -> referenced.contains(item.id()))) {
                hints.add("引用了误导性线索");
            }
        }
        if ("FINISHED".equals(room.getStatus())) {
            hints.add("游戏已结束，可查看结算与案件复盘");
        }

        String conclusion = submission == null ? "尚未提交推理结论"
                : "人物：" + submission.getCharacterAnswer() + "；时间：" + submission.getTimeAnswer()
                + "；地点：" + submission.getLocationAnswer() + "；行为：" + submission.getActionAnswer();

        return new ViewDtos.EvidenceChain(conclusion, visible, new ArrayList<>(referenced),
                support, key, indirect, contradict, misleading, unreferenced, hints,
                "FINISHED".equals(room.getStatus()));
    }

    // ---------------- 推理提交与结算 ----------------

    @Transactional
    public ViewDtos.SubmissionResult submit(Long roomId, Long userId,
                                            RequestDtos.SubmissionRequest request) {
        GameRoom room = requireRoom(roomId);
        if ("FINISHED".equals(room.getStatus())) {
            throw new BusinessException("游戏已结束，不能再次提交");
        }
        if (!"IN_PROGRESS".equals(room.getStatus())) {
            throw new BusinessException("当前房间状态不允许提交");
        }
        RoomPlayer me = requireMember(roomId, userId);
        if (submissionMapper.countByRoomAndUser(roomId, userId) > 0) {
            throw new BusinessException("你已经提交过最终推理，不能重复提交");
        }

        ScriptAnswer answer = answerMapper.selectByScriptId(room.getScriptId());
        if (answer == null) {
            throw new BusinessException("副本尚未配置标准答案，无法结算");
        }
        List<ScriptStage> stages = loadStages(room.getScriptId());
        Map<Long, Integer> stageOrders = clueAccess.stageOrderMap(stages);
        int currentOrder = clueAccess.currentOrder(room, stages);
        List<ScriptClue> allClues = clueMapper.selectByScriptId(room.getScriptId());
        Map<Long, ScriptClue> clueMap = allClues.stream()
                .collect(Collectors.toMap(ScriptClue::getId, Function.identity(), (a, b) -> a));

        List<RequestDtos.ClueRef> refs = request.getClues() == null ? List.of() : request.getClues();
        Set<Long> refIds = new HashSet<>();
        for (RequestDtos.ClueRef ref : refs) {
            ScriptClue clue = clueMap.get(ref.getClueId());
            if (clue == null) {
                throw new BusinessException("引用的线索不存在");
            }
            if (!clueAccess.isVisible(clue, me.getCharacterId(), stageOrders, currentOrder)) {
                throw new BusinessException("引用的线索不在当前可见范围内");
            }
            refIds.add(clue.getId());
        }

        GameSubmission submission = new GameSubmission();
        submission.setRoomId(roomId);
        submission.setUserId(userId);
        submission.setCharacterAnswer(request.getCharacterAnswer());
        submission.setTimeAnswer(request.getTimeAnswer());
        submission.setLocationAnswer(request.getLocationAnswer());
        submission.setActionAnswer(request.getActionAnswer());
        submission.setExplanation(request.getExplanation());
        submission.setSubmitTime(LocalDateTime.now());
        submission.setSubmissionOrder(1);
        submissionMapper.insert(submission);

        for (RequestDtos.ClueRef ref : refs) {
            if (!refIds.remove(ref.getClueId())) {
                continue;
            }
            SubmissionClue submissionClue = new SubmissionClue();
            submissionClue.setSubmissionId(submission.getId());
            submissionClue.setClueId(ref.getClueId());
            submissionClue.setRelationType(ref.getRelationType());
            submissionClueMapper.insert(submissionClue);
        }

        List<ClueRelation> relations = relationMapper.selectByScriptId(room.getScriptId());
        ScoringService.Outcome outcome = scoringService.evaluate(answer, request, allClues, relations);
        submission.setScore(outcome.score());
        submission.setScoreDetail(writeJson(outcome.detail()));
        submissionMapper.updateById(submission);

        unlockStage(room, stages, 2, "玩家提交推理答案");

        long submissionCount = submissionMapper.countByRoom(roomId);
        long memberCount = roomPlayerMapper.countByRoomId(roomId);
        if (submissionCount >= memberCount) {
            settle(room, stages);
        }
        return new ViewDtos.SubmissionResult(submission.getId(), outcome.score(), outcome.detail(),
                "FINISHED".equals(room.getStatus()), roomId);
    }

    // ---------------- 结算与复盘 ----------------

    public ViewDtos.ResultInfo getResult(Long roomId, Long userId) {
        GameRoom room = requireRoom(roomId);
        requireMember(roomId, userId);
        if (!"FINISHED".equals(room.getStatus())) {
            throw new BusinessException("游戏尚未结束，暂时无法查看结算");
        }
        GameResult mine = resultMapper.selectByRoomAndUser(roomId, userId);
        if (mine == null) {
            throw new BusinessException(404, "结算记录不存在");
        }
        GameScript script = scriptMapper.selectById(room.getScriptId());
        List<GameResult> results = resultMapper.selectByRoom(roomId);
        List<ViewDtos.PlayerScore> players = buildPlayerScores(room, results);

        List<ViewDtos.ClueItem> clues = allClueItems(room, userId);
        return new ViewDtos.ResultInfo(roomId, room.getScriptId(),
                script == null ? "" : script.getTitle(), mine.getScore(), mine.getResult(),
                mine.getScoreDetail(), mine.getHiddenGoal() == null ? null : mine.getHiddenGoal() == 1,
                players, clues);
    }

    public ViewDtos.ReplayInfo getReplay(Long roomId, Long userId) {
        GameRoom room = requireRoom(roomId);
        requireMember(roomId, userId);
        if (!"FINISHED".equals(room.getStatus())) {
            throw new BusinessException("游戏尚未结束，暂时无法查看复盘");
        }
        GameScript script = scriptMapper.selectById(room.getScriptId());
        ScriptAnswer answer = answerMapper.selectByScriptId(room.getScriptId());
        if (answer == null) {
            throw new BusinessException(404, "标准答案不存在");
        }
        List<ScriptClue> clueList = clueMapper.selectByScriptId(room.getScriptId());
        List<ScriptStage> stages = loadStages(room.getScriptId());
        Map<Long, String> stageNames = clueAccess.stageNameMap(stages);
        Set<Long> owned = playerClueMapper.selectByRoomAndUser(roomId, userId)
                .stream().map(PlayerClue::getClueId).collect(Collectors.toSet());
        List<ViewDtos.ClueItem> clues = clueList.stream()
                .map(clue -> clueAccess.toItem(clue, stageNames, owned.contains(clue.getId()), true))
                .toList();

        List<ClueRelation> relations = relationMapper.selectByScriptId(room.getScriptId());
        Map<Long, String> clueTitles = clueList.stream()
                .collect(Collectors.toMap(ScriptClue::getId, ScriptClue::getTitle, (a, b) -> a));
        List<ViewDtos.RelationItem> relationItems = relations.stream()
                .map(relation -> new ViewDtos.RelationItem(relation.getId(), relation.getClueId(),
                        clueTitles.getOrDefault(relation.getClueId(), "已删除线索"),
                        relation.getAnswerField(), relation.getRelationType(), relation.getScore()))
                .toList();

        List<GameResult> results = resultMapper.selectByRoom(roomId);
        List<ViewDtos.PlayerScore> players = buildPlayerScores(room, results);
        GameResult mine = results.stream().filter(r -> r.getUserId().equals(userId))
                .findFirst().orElse(null);

        List<String> timeline = clueList.stream()
                .filter(clue -> clue.getEventTime() != null && !clue.getEventTime().isBlank())
                .sorted(Comparator.comparing(ScriptClue::getEventTime))
                .map(clue -> clue.getEventTime() + " ｜ "
                        + (clue.getLocation() == null ? "未知地点" : clue.getLocation())
                        + " ｜ " + clue.getTitle())
                .toList();

        ViewDtos.AnswerInfo answerInfo = new ViewDtos.AnswerInfo(answer.getCharacterAnswer(),
                answer.getTimeAnswer(), answer.getLocationAnswer(), answer.getActionAnswer(),
                answer.getScoringRuleJson());
        return new ViewDtos.ReplayInfo(roomId, script == null ? "" : script.getTitle(),
                script == null ? "" : script.getStory(), answerInfo, clues, relationItems, players,
                mine == null ? null : mine.getScore(), mine == null ? null : mine.getScoreDetail(),
                timeline);
    }

    public List<ViewDtos.HistoryItem> history(Long userId) {
        List<GameResult> results = resultMapper.selectByUserRecent(userId, 50);
        if (results.isEmpty()) {
            return List.of();
        }
        Map<Long, GameRoom> rooms = roomMapper.selectByIds(results.stream()
                        .map(GameResult::getRoomId).distinct().toList())
                .stream().collect(Collectors.toMap(GameRoom::getId, Function.identity(), (a, b) -> a));
        Map<Long, GameScript> scripts = scriptMapper.selectByIds(rooms.values().stream()
                        .map(GameRoom::getScriptId).distinct().toList())
                .stream().collect(Collectors.toMap(GameScript::getId, Function.identity(), (a, b) -> a));
        List<Long> characterIds = results.stream()
                .map(GameResult::getCharacterId).filter(Objects::nonNull).distinct().toList();
        Map<Long, ScriptCharacter> characters = characterIds.isEmpty() ? new HashMap<>()
                : characterMapper.selectByIds(characterIds)
                .stream().collect(Collectors.toMap(ScriptCharacter::getId, Function.identity(), (a, b) -> a));
        return results.stream().map(result -> {
            GameRoom room = rooms.get(result.getRoomId());
            GameScript script = room == null ? null : scripts.get(room.getScriptId());
            ScriptCharacter character = result.getCharacterId() == null ? null
                    : characters.get(result.getCharacterId());
            return new ViewDtos.HistoryItem(result.getRoomId(),
                    room == null ? null : room.getScriptId(),
                    script == null ? "已删除副本" : script.getTitle(),
                    result.getScore(), result.getResult(),
                    character == null ? null : character.getCharacterName(),
                    room == null ? result.getCreateTime() : room.getFinishTime());
        }).toList();
    }

    public List<ViewDtos.AdminRoomItem> adminRooms() {
        List<GameRoom> rooms = roomMapper.selectRecent(200);
        if (rooms.isEmpty()) {
            return List.of();
        }
        Map<Long, GameScript> scripts = scriptMapper.selectByIds(rooms.stream()
                        .map(GameRoom::getScriptId).distinct().toList())
                .stream().collect(Collectors.toMap(GameScript::getId, Function.identity(), (a, b) -> a));
        Map<Long, Long> counts = roomPlayerMapper.selectByRoomIds(
                        rooms.stream().map(GameRoom::getId).toList())
                .stream().collect(Collectors.groupingBy(RoomPlayer::getRoomId, Collectors.counting()));
        return rooms.stream().map(room -> {
            GameScript script = scripts.get(room.getScriptId());
            return new ViewDtos.AdminRoomItem(room.getId(), room.getScriptId(),
                    script == null ? "已删除副本" : script.getTitle(), room.getStatus(),
                    counts.getOrDefault(room.getId(), 0L).intValue(), room.getCreateTime(),
                    room.getFinishTime());
        }).toList();
    }

    // ---------------- 内部方法 ----------------

    private GameRoom requireRoom(Long roomId) {
        GameRoom room = roomMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException(404, "房间不存在");
        }
        return room;
    }

    private RoomPlayer requireMember(Long roomId, Long userId) {
        RoomPlayer member = roomPlayerMapper.selectByRoomAndUser(roomId, userId);
        if (member == null) {
            throw new BusinessException(403, "你不是该房间的玩家");
        }
        return member;
    }

    private List<ScriptStage> loadStages(Long scriptId) {
        return stageMapper.selectByScriptId(scriptId);
    }

    private Map<Long, ScriptCharacter> loadCharacters(Long scriptId) {
        return characterMapper.selectByScriptId(scriptId)
                .stream().collect(Collectors.toMap(ScriptCharacter::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, SysUser> loadUsers(List<RoomPlayer> members) {
        if (members.isEmpty()) {
            return new HashMap<>();
        }
        return userMapper.selectByIds(members.stream().map(RoomPlayer::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));
    }

    private GameSubmission latestSubmission(Long roomId, Long userId) {
        return submissionMapper.selectLatestByRoomAndUser(roomId, userId);
    }

    private void unlockStage(GameRoom room, List<ScriptStage> stages, int order, String reason) {
        ScriptStage target = stages.stream()
                .filter(stage -> Integer.valueOf(order).equals(stage.getStageOrder()))
                .findFirst().orElse(null);
        if (target == null) {
            return;
        }
        RoomStageRecord record = stageRecordMapper.selectByRoomAndStage(room.getId(), target.getId());
        if (record == null) {
            record = new RoomStageRecord();
            record.setRoomId(room.getId());
            record.setStageId(target.getId());
            record.setUnlockTime(LocalDateTime.now());
            record.setUnlockReason(reason);
            record.setIsOpen(1);
            stageRecordMapper.insert(record);
        } else if (!Integer.valueOf(1).equals(record.getIsOpen())) {
            record.setIsOpen(1);
            record.setUnlockReason(reason);
            stageRecordMapper.updateById(record);
        }
        int currentOrder = clueAccess.currentOrder(room, stages);
        if (order > currentOrder) {
            room.setCurrentStageId(target.getId());
            roomMapper.updateById(room);
        }
        distributeClues(room, stages, Math.max(order, currentOrder));
    }

    private void distributeClues(GameRoom room, List<ScriptStage> stages, int maxOrder) {
        List<ScriptClue> clues = clueMapper.selectByScriptId(room.getScriptId());
        Map<Long, Integer> stageOrders = clueAccess.stageOrderMap(stages);
        List<RoomPlayer> players = roomPlayerMapper.selectByRoomId(room.getId());
        for (RoomPlayer player : players) {
            for (ScriptClue clue : clues) {
                if (!clueAccess.isVisible(clue, player.getCharacterId(), stageOrders, maxOrder)) {
                    continue;
                }
                if (playerClueMapper.countByRoomUserClue(room.getId(), player.getUserId(),
                        clue.getId()) > 0) {
                    continue;
                }
                PlayerClue playerClue = new PlayerClue();
                playerClue.setRoomId(room.getId());
                playerClue.setUserId(player.getUserId());
                playerClue.setClueId(clue.getId());
                playerClue.setIsRead(0);
                playerClueMapper.insert(playerClue);
            }
        }
    }

    private void settle(GameRoom room, List<ScriptStage> stages) {
        if ("FINISHED".equals(room.getStatus())) {
            return;
        }
        ScriptAnswer answer = answerMapper.selectByScriptId(room.getScriptId());
        List<GameSubmission> submissions = submissionMapper.selectByRoom(room.getId());
        Map<Long, GameSubmission> byUser = submissions.stream()
                .collect(Collectors.toMap(GameSubmission::getUserId, Function.identity(), (a, b) -> a));

        List<RoomPlayer> members = roomPlayerMapper.selectByRoomId(room.getId());
        Map<Long, ScriptCharacter> characters = loadCharacters(room.getScriptId());
        List<ScriptCharacter> hidden = characters.values().stream()
                .filter(character -> Integer.valueOf(1).equals(character.getIsHidden())).toList();

        boolean hiddenIdentified = false;
        for (ScriptCharacter hiddenCharacter : hidden) {
            for (RoomPlayer member : members) {
                ScriptCharacter memberCharacter = characters.get(member.getCharacterId());
                if (memberCharacter != null && Integer.valueOf(1).equals(memberCharacter.getIsHidden())) {
                    continue;
                }
                GameSubmission submission = byUser.get(member.getUserId());
                if (submission != null && scoringService.matchesAnswer(
                        submission.getCharacterAnswer(), hiddenCharacter.getCharacterName())) {
                    hiddenIdentified = true;
                }
            }
        }

        for (RoomPlayer member : members) {
            GameSubmission submission = byUser.get(member.getUserId());
            if (submission == null) {
                continue;
            }
            if (resultMapper.countByRoomAndUser(room.getId(), member.getUserId()) > 0) {
                continue;
            }
            ScriptCharacter character = characters.get(member.getCharacterId());
            GameResult result = new GameResult();
            result.setRoomId(room.getId());
            result.setUserId(member.getUserId());
            result.setCharacterId(member.getCharacterId());
            result.setFinalAnswer("人物：" + submission.getCharacterAnswer()
                    + "；时间：" + submission.getTimeAnswer()
                    + "；地点：" + submission.getLocationAnswer()
                    + "；行为：" + submission.getActionAnswer());
            result.setScore(submission.getScore() == null ? 0 : submission.getScore());
            result.setScoreDetail(submission.getScoreDetail());
            result.setResult(scoringService.resultOfScore(result.getScore()));
            if (character != null && Integer.valueOf(1).equals(character.getIsHidden())) {
                result.setHiddenGoal(hiddenIdentified ? 0 : 1);
            }
            resultMapper.insert(result);
        }

        room.setStatus("FINISHED");
        room.setFinishTime(LocalDateTime.now());
        roomMapper.updateById(room);

        for (ScriptStage stage : stages) {
            RoomStageRecord record = stageRecordMapper.selectByRoomAndStage(room.getId(), stage.getId());
            if (record == null) {
                record = new RoomStageRecord();
                record.setRoomId(room.getId());
                record.setStageId(stage.getId());
                record.setUnlockTime(LocalDateTime.now());
                record.setUnlockReason("游戏结算，复盘阶段");
                record.setIsOpen(1);
                stageRecordMapper.insert(record);
            } else if (!Integer.valueOf(1).equals(record.getIsOpen())) {
                record.setIsOpen(1);
                record.setUnlockReason("游戏结算，复盘阶段");
                stageRecordMapper.updateById(record);
            }
        }
        if (answer == null) {
            log.warn("房间 {} 结算时未找到标准答案", room.getId());
        }
    }

    private List<ViewDtos.PlayerScore> buildPlayerScores(GameRoom room, List<GameResult> results) {
        Map<Long, RoomPlayer> members = roomPlayerMapper.selectByRoomId(room.getId())
                .stream().collect(Collectors.toMap(RoomPlayer::getUserId, Function.identity(), (a, b) -> a));
        Map<Long, SysUser> users = loadUsers(new ArrayList<>(members.values()));
        Map<Long, ScriptCharacter> characters = loadCharacters(room.getScriptId());
        return results.stream()
                .sorted(Comparator.comparing((GameResult result) ->
                        result.getScore() == null ? 0 : result.getScore()).reversed())
                .map(result -> {
                    RoomPlayer member = members.get(result.getUserId());
                    SysUser user = users.get(result.getUserId());
                    ScriptCharacter character = member == null ? null
                            : characters.get(member.getCharacterId());
                    return new ViewDtos.PlayerScore(result.getUserId(),
                            user == null ? "未知玩家" : user.getNickname(),
                            character == null ? "未知角色" : character.getCharacterName(),
                            result.getScore(), result.getResult());
                }).toList();
    }

    private List<ViewDtos.ClueItem> allClueItems(GameRoom room, Long userId) {
        List<ScriptStage> stages = loadStages(room.getScriptId());
        Map<Long, String> stageNames = clueAccess.stageNameMap(stages);
        Set<Long> owned = playerClueMapper.selectByRoomAndUser(room.getId(), userId)
                .stream().map(PlayerClue::getClueId).collect(Collectors.toSet());
        return clueMapper.selectByScriptId(room.getScriptId())
                .stream().map(clue -> clueAccess.toItem(clue, stageNames,
                        owned.contains(clue.getId()), true))
                .toList();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("评分明细序列化失败", e);
            return "{}";
        }
    }
}
