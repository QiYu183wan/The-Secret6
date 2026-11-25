package com.enigma.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 接口视图对象定义。
 */
public final class ViewDtos {

    private ViewDtos() {
    }

    public record UserInfo(Long id, String username, String nickname, String avatar,
                           String bio, String role, String status) {
    }

    public record ScriptCard(Long id, String title, String description, String story,
                             String difficulty, Integer minPlayers, Integer maxPlayers,
                             Integer requiredPlayers, Integer estimatedMinutes,
                             String status, String coverImage, Integer characterCount,
                             Integer clueCount, Long joinCount) {
    }

    public record StageItem(Long id, String stageName, String stageDescription,
                            Integer stageOrder, String unlockRule, Integer timeoutMinutes) {
    }

    public record CharacterItem(Long id, String characterName, String description,
                                String characterGoal, Integer isHidden, Integer sortOrder) {
    }

    public record ScriptDetail(Long id, String title, String description, String story,
                               String difficulty, Integer minPlayers, Integer maxPlayers,
                               Integer requiredPlayers, Integer estimatedMinutes,
                               String status, String coverImage, LocalDateTime createTime,
                               LocalDateTime updateTime, List<StageItem> stages,
                               List<CharacterItem> characters, Integer clueCount,
                               Boolean hasAnswer) {
    }

    public record ClueItem(Long id, Long stageId, String stageName, String title, String content,
                           String visibilityType, Long visibleCharacterId, String eventTime,
                           String location, Integer credibility, Integer isKeyClue,
                           Integer isMisleading, boolean mine, boolean read) {
    }

    public record RoomPlayerItem(Long userId, String nickname, Long characterId,
                                 String characterName, boolean hidden, LocalDateTime joinTime) {
    }

    public record RoomInfo(Long id, Long scriptId, String scriptTitle, String status,
                           Long currentStageId, Integer currentStageOrder, String currentStageName,
                           LocalDateTime createTime, LocalDateTime startTime, LocalDateTime finishTime,
                           List<RoomPlayerItem> players, List<String> characterNames,
                           boolean mySubmitted, boolean allSubmitted, Integer myScore) {
    }

    public record MyCharacter(Long id, String characterName, String description,
                              String goal, boolean hidden) {
    }

    public record StageProgressItem(Long id, String stageName, String stageDescription,
                                    Integer stageOrder, boolean open, LocalDateTime unlockTime) {
    }

    public record StageProgress(Long currentStageId, Integer currentStageOrder,
                                List<StageProgressItem> stages) {
    }

    public record MatchStatus(Long scriptId, String status, Long roomId, Integer waitingCount,
                              Integer requiredCount, LocalDateTime createTime) {
    }

    public record SubmissionResult(Long submissionId, Integer score, Object scoreDetail,
                                   boolean roomFinished, Long roomId) {
    }

    public record PlayerScore(Long userId, String nickname, String characterName,
                              Integer score, String result) {
    }

    public record ResultInfo(Long roomId, Long scriptId, String scriptTitle, Integer myScore,
                             String result, String scoreDetail, Boolean hiddenGoal,
                             List<PlayerScore> players, List<ClueItem> clues) {
    }

    public record AnswerInfo(String characterAnswer, String timeAnswer, String locationAnswer,
                             String actionAnswer, String scoringRuleJson) {
    }

    public record RelationItem(Long id, Long clueId, String clueTitle, String answerField,
                               String relationType, Integer score) {
    }

    public record ReplayInfo(Long roomId, String scriptTitle, String story, AnswerInfo standardAnswer,
                             List<ClueItem> clues, List<RelationItem> relations,
                             List<PlayerScore> players, Integer myScore, String myScoreDetail,
                             List<String> timeline) {
    }

    public record EvidenceChain(String conclusion, List<ClueItem> visibleClues,
                                List<Long> referencedClueIds, List<ClueItem> supportClues,
                                List<ClueItem> keyClues, List<ClueItem> indirectClues,
                                List<ClueItem> contradictClues, List<ClueItem> misleadingClues,
                                List<ClueItem> unreferencedClues, List<String> hints,
                                boolean finished) {
    }

    public record HistoryItem(Long roomId, Long scriptId, String scriptTitle, Integer score,
                              String result, String characterName, LocalDateTime finishTime) {
    }

    public record StatisticsInfo(Long scriptId, String scriptTitle, long rooms, long finishedRooms,
                                 long players, double averageScore) {
    }

    public record AdminRoomItem(Long id, Long scriptId, String scriptTitle, String status,
                                int playerCount, LocalDateTime createTime, LocalDateTime finishTime) {
    }
}
