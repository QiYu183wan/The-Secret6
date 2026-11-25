package com.enigma.service;

import com.enigma.common.BusinessException;
import com.enigma.dto.RequestDtos;
import com.enigma.dto.ViewDtos;
import com.enigma.entity.ClueRelation;
import com.enigma.entity.GameResult;
import com.enigma.entity.GameRoom;
import com.enigma.entity.GameScript;
import com.enigma.entity.ScriptAnswer;
import com.enigma.entity.ScriptCharacter;
import com.enigma.entity.ScriptClue;
import com.enigma.entity.ScriptStage;
import com.enigma.mapper.ClueRelationMapper;
import com.enigma.mapper.GameResultMapper;
import com.enigma.mapper.GameRoomMapper;
import com.enigma.mapper.GameScriptMapper;
import com.enigma.mapper.MatchmakingTicketMapper;
import com.enigma.mapper.ScriptAnswerMapper;
import com.enigma.mapper.ScriptCharacterMapper;
import com.enigma.mapper.ScriptClueMapper;
import com.enigma.mapper.ScriptStageMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ScriptService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final GameScriptMapper scriptMapper;
    private final ScriptCharacterMapper characterMapper;
    private final ScriptStageMapper stageMapper;
    private final ScriptClueMapper clueMapper;
    private final ClueRelationMapper relationMapper;
    private final ScriptAnswerMapper answerMapper;
    private final MatchmakingTicketMapper ticketMapper;
    private final GameRoomMapper roomMapper;
    private final GameResultMapper resultMapper;
    private final ScriptCacheService cache;
    private final ClueAccessService clueAccess;

    // ---------------- 玩家端 ----------------

    public List<ViewDtos.ScriptCard> listPublished() {
        List<ViewDtos.ScriptCard> cached = cache.get(ScriptCacheService.PUBLISHED_LIST_KEY,
                new TypeReference<List<ViewDtos.ScriptCard>>() {
                });
        if (cached != null) {
            return cached;
        }
        List<ViewDtos.ScriptCard> cards = scriptMapper.selectPublished()
                .stream().map(this::toCard).toList();
        cache.put(ScriptCacheService.PUBLISHED_LIST_KEY, cards, CACHE_TTL);
        return cards;
    }

    public ViewDtos.ScriptDetail getPublishedDetail(Long scriptId) {
        String key = cache.detailKey(scriptId);
        ViewDtos.ScriptDetail cached = cache.get(key,
                new TypeReference<ViewDtos.ScriptDetail>() {
                });
        if (cached != null) {
            return cached;
        }
        GameScript script = requireScript(scriptId);
        if (!"PUBLISHED".equals(script.getStatus())) {
            throw new BusinessException(404, "副本不存在或未发布");
        }
        ViewDtos.ScriptDetail detail = buildDetail(script, false);
        cache.put(key, detail, CACHE_TTL);
        return detail;
    }

    // ---------------- 管理端 ----------------

    public List<ViewDtos.ScriptCard> listAll() {
        return scriptMapper.selectAllOrderByIdDesc()
                .stream().map(this::toCard).toList();
    }

    public ViewDtos.ScriptDetail adminDetail(Long scriptId) {
        return buildDetail(requireScript(scriptId), true);
    }

    @Transactional
    public ViewDtos.ScriptDetail create(RequestDtos.ScriptForm form) {
        validateForm(form);
        GameScript script = new GameScript();
        applyForm(script, form);
        script.setStatus("DRAFT");
        scriptMapper.insert(script);
        cache.evictScript(script.getId());
        return buildDetail(script, true);
    }

    @Transactional
    public ViewDtos.ScriptDetail update(Long scriptId, RequestDtos.ScriptForm form) {
        validateForm(form);
        GameScript script = requireScript(scriptId);
        applyForm(script, form);
        scriptMapper.updateById(script);
        cache.evictScript(scriptId);
        return buildDetail(script, true);
    }

    @Transactional
    public void publish(Long scriptId) {
        GameScript script = requireScript(scriptId);
        ScriptAnswer answer = answerMapper.selectByScriptId(scriptId);
        if (answer == null) {
            throw new BusinessException("发布失败：请先配置标准答案和评分规则");
        }
        List<ScriptCharacter> characters = characterMapper.selectByScriptId(scriptId);
        if (characters.size() != script.getRequiredPlayers()) {
            throw new BusinessException("发布失败：开局人数必须与角色数量一致（当前角色 "
                    + characters.size() + " 个，开局人数 " + script.getRequiredPlayers() + "）");
        }
        List<ScriptClue> clues = clueMapper.selectByScriptId(scriptId);
        boolean hasPublic = clues.stream().anyMatch(c -> "PUBLIC".equals(c.getVisibilityType()));
        if (!hasPublic) {
            throw new BusinessException("发布失败：至少需要一条公开线索");
        }
        List<ClueRelation> relations = relationMapper.selectByScriptId(scriptId);
        boolean hasKey = clues.stream().anyMatch(c -> Integer.valueOf(1).equals(c.getIsKeyClue()))
                || relations.stream().anyMatch(r -> "KEY".equals(r.getRelationType()));
        if (!hasKey) {
            throw new BusinessException("发布失败：至少需要一条关键线索");
        }
        script.setStatus("PUBLISHED");
        scriptMapper.updateById(script);
        cache.evictScript(scriptId);
    }

    @Transactional
    public void offline(Long scriptId) {
        GameScript script = requireScript(scriptId);
        script.setStatus("OFFLINE");
        scriptMapper.updateById(script);
        cache.evictScript(scriptId);
    }

    @Transactional
    public void delete(Long scriptId) {
        requireScript(scriptId);
        scriptMapper.deleteById(scriptId);
        cache.evictScript(scriptId);
    }

    // ---------------- 角色 ----------------

    public List<ViewDtos.CharacterItem> listCharacters(Long scriptId) {
        requireScript(scriptId);
        return characterMapper.selectByScriptId(scriptId)
                .stream().map(this::toCharacterItem).toList();
    }

    @Transactional
    public ViewDtos.CharacterItem createCharacter(Long scriptId, RequestDtos.CharacterForm form) {
        requireScript(scriptId);
        ScriptCharacter character = new ScriptCharacter();
        character.setScriptId(scriptId);
        applyCharacter(character, form);
        characterMapper.insert(character);
        cache.evictScript(scriptId);
        return toCharacterItem(character);
    }

    @Transactional
    public ViewDtos.CharacterItem updateCharacter(Long scriptId, Long characterId,
                                                  RequestDtos.CharacterForm form) {
        ScriptCharacter character = requireCharacter(scriptId, characterId);
        applyCharacter(character, form);
        characterMapper.updateById(character);
        cache.evictScript(scriptId);
        return toCharacterItem(character);
    }

    @Transactional
    public void deleteCharacter(Long scriptId, Long characterId) {
        requireCharacter(scriptId, characterId);
        characterMapper.deleteById(characterId);
        cache.evictScript(scriptId);
    }

    // ---------------- 阶段 ----------------

    public List<ViewDtos.StageItem> listStages(Long scriptId) {
        requireScript(scriptId);
        return stageMapper.selectByScriptId(scriptId)
                .stream().map(this::toStageItem).toList();
    }

    @Transactional
    public ViewDtos.StageItem createStage(Long scriptId, RequestDtos.StageForm form) {
        requireScript(scriptId);
        ScriptStage stage = new ScriptStage();
        stage.setScriptId(scriptId);
        applyStage(stage, form);
        stageMapper.insert(stage);
        cache.evictScript(scriptId);
        return toStageItem(stage);
    }

    @Transactional
    public ViewDtos.StageItem updateStage(Long scriptId, Long stageId, RequestDtos.StageForm form) {
        ScriptStage stage = stageMapper.selectById(stageId);
        if (stage == null || !Objects.equals(stage.getScriptId(), scriptId)) {
            throw new BusinessException(404, "阶段不存在");
        }
        applyStage(stage, form);
        stageMapper.updateById(stage);
        cache.evictScript(scriptId);
        return toStageItem(stage);
    }

    @Transactional
    public void deleteStage(Long scriptId, Long stageId) {
        ScriptStage stage = stageMapper.selectById(stageId);
        if (stage == null || !Objects.equals(stage.getScriptId(), scriptId)) {
            throw new BusinessException(404, "阶段不存在");
        }
        stageMapper.deleteById(stageId);
        cache.evictScript(scriptId);
    }

    // ---------------- 线索 ----------------

    public List<ViewDtos.ClueItem> listClues(Long scriptId) {
        requireScript(scriptId);
        List<ScriptStage> stages = stageMapper.selectByScriptId(scriptId);
        var stageNames = clueAccess.stageNameMap(stages);
        return clueMapper.selectByScriptId(scriptId)
                .stream().map(c -> clueAccess.toItem(c, stageNames, false, false)).toList();
    }

    @Transactional
    public ViewDtos.ClueItem createClue(Long scriptId, RequestDtos.ClueForm form) {
        requireScript(scriptId);
        ScriptClue clue = new ScriptClue();
        clue.setScriptId(scriptId);
        applyClue(clue, form, scriptId);
        clueMapper.insert(clue);
        cache.evictScript(scriptId);
        return toClueItem(clue, scriptId);
    }

    @Transactional
    public ViewDtos.ClueItem updateClue(Long scriptId, Long clueId, RequestDtos.ClueForm form) {
        ScriptClue clue = requireClue(scriptId, clueId);
        applyClue(clue, form, scriptId);
        clueMapper.updateById(clue);
        cache.evictScript(scriptId);
        return toClueItem(clue, scriptId);
    }

    @Transactional
    public void deleteClue(Long scriptId, Long clueId) {
        requireClue(scriptId, clueId);
        clueMapper.deleteById(clueId);
        cache.evictScript(scriptId);
    }

    // ---------------- 标准答案 ----------------

    public ViewDtos.AnswerInfo getAnswer(Long scriptId) {
        requireScript(scriptId);
        ScriptAnswer answer = answerMapper.selectByScriptId(scriptId);
        if (answer == null) {
            return null;
        }
        return new ViewDtos.AnswerInfo(answer.getCharacterAnswer(), answer.getTimeAnswer(),
                answer.getLocationAnswer(), answer.getActionAnswer(), answer.getScoringRuleJson());
    }

    @Transactional
    public ViewDtos.AnswerInfo saveAnswer(Long scriptId, RequestDtos.AnswerForm form) {
        requireScript(scriptId);
        ScriptAnswer answer = answerMapper.selectByScriptId(scriptId);
        if (answer == null) {
            answer = new ScriptAnswer();
            answer.setScriptId(scriptId);
            applyAnswer(answer, form);
            answerMapper.insert(answer);
        } else {
            applyAnswer(answer, form);
            answerMapper.updateById(answer);
        }
        cache.evictScript(scriptId);
        return new ViewDtos.AnswerInfo(answer.getCharacterAnswer(), answer.getTimeAnswer(),
                answer.getLocationAnswer(), answer.getActionAnswer(), answer.getScoringRuleJson());
    }

    // ---------------- 证据关系 ----------------

    public List<ViewDtos.RelationItem> listRelations(Long scriptId) {
        requireScript(scriptId);
        List<ClueRelation> relations = relationMapper.selectByScriptId(scriptId);
        List<ScriptClue> clues = clueMapper.selectByScriptId(scriptId);
        return relations.stream().map(r -> {
            String clueTitle = clues.stream().filter(c -> c.getId().equals(r.getClueId()))
                    .map(ScriptClue::getTitle).findFirst().orElse("已删除线索");
            return new ViewDtos.RelationItem(r.getId(), r.getClueId(), clueTitle,
                    r.getAnswerField(), r.getRelationType(), r.getScore());
        }).toList();
    }

    @Transactional
    public ViewDtos.RelationItem createRelation(Long scriptId, RequestDtos.RelationForm form) {
        requireScript(scriptId);
        ScriptClue clue = requireClue(scriptId, form.getClueId());
        validateRelation(form);
        ClueRelation relation = new ClueRelation();
        relation.setScriptId(scriptId);
        relation.setClueId(form.getClueId());
        relation.setAnswerField(form.getAnswerField());
        relation.setRelationType(form.getRelationType());
        relation.setScore(form.getScore() == null ? 0 : form.getScore());
        relationMapper.insert(relation);
        cache.evictScript(scriptId);
        return new ViewDtos.RelationItem(relation.getId(), relation.getClueId(), clue.getTitle(),
                relation.getAnswerField(), relation.getRelationType(), relation.getScore());
    }

    @Transactional
    public void deleteRelation(Long scriptId, Long relationId) {
        ClueRelation relation = relationMapper.selectById(relationId);
        if (relation == null || !Objects.equals(relation.getScriptId(), scriptId)) {
            throw new BusinessException(404, "证据关系不存在");
        }
        relationMapper.deleteById(relationId);
        cache.evictScript(scriptId);
    }

    // ---------------- 统计 ----------------

    public ViewDtos.StatisticsInfo statistics(Long scriptId) {
        GameScript script = requireScript(scriptId);
        long rooms = roomMapper.countByScriptId(scriptId);
        long finishedRooms = roomMapper.countByScriptIdAndStatus(scriptId, "FINISHED");
        List<GameRoom> roomList = roomMapper.selectByScriptId(scriptId);
        if (roomList.isEmpty()) {
            return new ViewDtos.StatisticsInfo(scriptId, script.getTitle(), rooms, finishedRooms, 0, 0D);
        }
        List<Long> roomIds = roomList.stream().map(GameRoom::getId).toList();
        List<GameResult> results = resultMapper.selectByRoomIds(roomIds);
        long players = results.stream().map(GameResult::getUserId).distinct().count();
        double average = results.stream().mapToInt(r -> r.getScore() == null ? 0 : r.getScore())
                .average().orElse(0D);
        return new ViewDtos.StatisticsInfo(scriptId, script.getTitle(), rooms, finishedRooms, players,
                Math.round(average * 10) / 10.0);
    }

    // ---------------- 内部方法 ----------------

    private GameScript requireScript(Long scriptId) {
        GameScript script = scriptMapper.selectById(scriptId);
        if (script == null) {
            throw new BusinessException(404, "副本不存在");
        }
        return script;
    }

    private ScriptCharacter requireCharacter(Long scriptId, Long characterId) {
        ScriptCharacter character = characterMapper.selectById(characterId);
        if (character == null || !Objects.equals(character.getScriptId(), scriptId)) {
            throw new BusinessException(404, "角色不存在");
        }
        return character;
    }

    private ScriptClue requireClue(Long scriptId, Long clueId) {
        ScriptClue clue = clueMapper.selectById(clueId);
        if (clue == null || !Objects.equals(clue.getScriptId(), scriptId)) {
            throw new BusinessException(404, "线索不存在");
        }
        return clue;
    }

    private void validateForm(RequestDtos.ScriptForm form) {
        int min = form.getMinPlayers() == null ? 2 : form.getMinPlayers();
        int max = form.getMaxPlayers() == null ? 4 : form.getMaxPlayers();
        int required = form.getRequiredPlayers() == null ? 4 : form.getRequiredPlayers();
        if (min > max) {
            throw new BusinessException("最少玩家人数不能大于最大玩家人数");
        }
        if (required < min || required > max) {
            throw new BusinessException("开局人数必须介于最少人数和最大人数之间");
        }
    }

    private void validateRelation(RequestDtos.RelationForm form) {
        List<String> fields = List.of("CHARACTER", "TIME", "LOCATION", "ACTION", "KEY");
        List<String> types = List.of("SUPPORT", "CONTRADICT", "INDIRECT", "INSUFFICIENT", "KEY", "MISLEADING");
        if (!fields.contains(form.getAnswerField())) {
            throw new BusinessException("答案字段不合法");
        }
        if (!types.contains(form.getRelationType())) {
            throw new BusinessException("关系类型不合法");
        }
    }

    private void applyForm(GameScript script, RequestDtos.ScriptForm form) {
        script.setTitle(form.getTitle().trim());
        script.setDescription(form.getDescription());
        script.setStory(form.getStory());
        script.setDifficulty(form.getDifficulty() == null ? "MEDIUM" : form.getDifficulty());
        script.setMinPlayers(form.getMinPlayers() == null ? 2 : form.getMinPlayers());
        script.setMaxPlayers(form.getMaxPlayers() == null ? 4 : form.getMaxPlayers());
        script.setRequiredPlayers(form.getRequiredPlayers() == null ? 4 : form.getRequiredPlayers());
        script.setEstimatedMinutes(form.getEstimatedMinutes() == null ? 60 : form.getEstimatedMinutes());
        script.setCoverImage(form.getCoverImage());
    }

    private void applyCharacter(ScriptCharacter character, RequestDtos.CharacterForm form) {
        character.setCharacterName(form.getCharacterName().trim());
        character.setDescription(form.getDescription());
        character.setCharacterGoal(form.getCharacterGoal());
        character.setIsHidden(form.getIsHidden() == null ? 0 : form.getIsHidden());
        character.setSortOrder(form.getSortOrder() == null ? 0 : form.getSortOrder());
    }

    private void applyStage(ScriptStage stage, RequestDtos.StageForm form) {
        stage.setStageName(form.getStageName().trim());
        stage.setStageDescription(form.getStageDescription());
        stage.setStageOrder(form.getStageOrder());
        stage.setUnlockRule(form.getUnlockRule());
        stage.setTimeoutMinutes(form.getTimeoutMinutes());
    }

    private void applyClue(ScriptClue clue, RequestDtos.ClueForm form, Long scriptId) {
        if (form.getStageId() != null) {
            ScriptStage stage = stageMapper.selectById(form.getStageId());
            if (stage == null || !Objects.equals(stage.getScriptId(), scriptId)) {
                throw new BusinessException("阶段不属于当前副本");
            }
        }
        if (form.getVisibleCharacterId() != null) {
            ScriptCharacter character = characterMapper.selectById(form.getVisibleCharacterId());
            if (character == null || !Objects.equals(character.getScriptId(), scriptId)) {
                throw new BusinessException("可见角色不属于当前副本");
            }
        }
        clue.setStageId(form.getStageId());
        clue.setTitle(form.getTitle().trim());
        clue.setContent(form.getContent());
        clue.setVisibilityType(form.getVisibilityType() == null ? "PUBLIC" : form.getVisibilityType());
        clue.setVisibleCharacterId(form.getVisibleCharacterId());
        clue.setEventTime(form.getEventTime());
        clue.setLocation(form.getLocation());
        clue.setCredibility(form.getCredibility() == null ? 5 : form.getCredibility());
        clue.setIsKeyClue(form.getIsKeyClue() == null ? 0 : form.getIsKeyClue());
        clue.setIsMisleading(form.getIsMisleading() == null ? 0 : form.getIsMisleading());
        clue.setSortOrder(form.getSortOrder() == null ? 0 : form.getSortOrder());
    }

    private void applyAnswer(ScriptAnswer answer, RequestDtos.AnswerForm form) {
        answer.setCharacterAnswer(form.getCharacterAnswer());
        answer.setTimeAnswer(form.getTimeAnswer());
        answer.setLocationAnswer(form.getLocationAnswer());
        answer.setActionAnswer(form.getActionAnswer());
        answer.setScoringRuleJson(form.getScoringRuleJson());
    }

    private ViewDtos.ScriptCard toCard(GameScript script) {
        int charCount = (int) characterMapper.countByScriptId(script.getId());
        int clueCount = (int) clueMapper.countByScriptId(script.getId());
        long joinCount = ticketMapper.countWaiting(script.getId());
        return new ViewDtos.ScriptCard(script.getId(), script.getTitle(), script.getDescription(),
                script.getStory(), script.getDifficulty(), script.getMinPlayers(), script.getMaxPlayers(),
                script.getRequiredPlayers(), script.getEstimatedMinutes(), script.getStatus(),
                script.getCoverImage(), charCount, clueCount, joinCount);
    }

    private ViewDtos.ScriptDetail buildDetail(GameScript script, boolean includeGoal) {
        List<ScriptStage> stages = stageMapper.selectByScriptId(script.getId());
        List<ScriptCharacter> characters = characterMapper.selectByScriptId(script.getId());
        int clueCount = (int) clueMapper.countByScriptId(script.getId());
        long answerCount = answerMapper.countByScriptId(script.getId());
        List<ViewDtos.StageItem> stageItems = stages.stream().map(this::toStageItem).toList();
        List<ViewDtos.CharacterItem> characterItems = characters.stream()
                .map(c -> new ViewDtos.CharacterItem(c.getId(), c.getCharacterName(), c.getDescription(),
                        includeGoal ? c.getCharacterGoal() : null, c.getIsHidden(), c.getSortOrder()))
                .toList();
        return new ViewDtos.ScriptDetail(script.getId(), script.getTitle(), script.getDescription(),
                script.getStory(), script.getDifficulty(), script.getMinPlayers(), script.getMaxPlayers(),
                script.getRequiredPlayers(), script.getEstimatedMinutes(), script.getStatus(),
                script.getCoverImage(), script.getCreateTime(), script.getUpdateTime(),
                stageItems, characterItems, clueCount, answerCount > 0);
    }

    private ViewDtos.CharacterItem toCharacterItem(ScriptCharacter character) {
        return new ViewDtos.CharacterItem(character.getId(), character.getCharacterName(),
                character.getDescription(), character.getCharacterGoal(),
                character.getIsHidden(), character.getSortOrder());
    }

    private ViewDtos.StageItem toStageItem(ScriptStage stage) {
        return new ViewDtos.StageItem(stage.getId(), stage.getStageName(), stage.getStageDescription(),
                stage.getStageOrder(), stage.getUnlockRule(), stage.getTimeoutMinutes());
    }

    private ViewDtos.ClueItem toClueItem(ScriptClue clue, Long scriptId) {
        List<ScriptStage> stages = stageMapper.selectByScriptId(scriptId);
        return clueAccess.toItem(clue, clueAccess.stageNameMap(stages), false, false);
    }
}
