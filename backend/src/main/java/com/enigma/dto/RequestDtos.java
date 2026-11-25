package com.enigma.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 请求体定义。
 */
public final class RequestDtos {

    private RequestDtos() {
    }

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 32, message = "长度需在3到32之间")
        private String username;

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 32, message = "长度需在6到32之间")
        private String password;

        @NotBlank(message = "昵称不能为空")
        @Size(max = 32, message = "长度不能超过32")
        private String nickname;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class ProfileRequest {
        @NotBlank(message = "昵称不能为空")
        @Size(max = 32, message = "长度不能超过32")
        private String nickname;
        @Size(max = 256, message = "长度不能超过256")
        private String avatar;
        @Size(max = 512, message = "长度不能超过512")
        private String bio;
    }

    @Data
    public static class PasswordRequest {
        @NotBlank(message = "当前密码不能为空")
        private String oldPassword;
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 32, message = "新密码长度需在6到32之间")
        private String newPassword;
    }

    @Data
    public static class ScriptForm {
        @NotBlank(message = "副本名称不能为空")
        @Size(max = 128, message = "长度不能超过128")
        private String title;
        private String description;
        private String story;
        private String difficulty = "MEDIUM";
        @Min(value = 1, message = "最少玩家数不能小于1")
        private Integer minPlayers = 2;
        @Min(value = 1, message = "最大玩家数不能小于1")
        @Max(value = 12, message = "最大玩家数不能超过12")
        private Integer maxPlayers = 4;
        @Min(value = 1, message = "开局人数至少为1")
        private Integer requiredPlayers = 4;
        @Min(value = 1, message = "预计时长至少为1分钟")
        private Integer estimatedMinutes = 60;
        private String coverImage;
    }

    @Data
    public static class CharacterForm {
        @NotBlank(message = "角色名称不能为空")
        @Size(max = 64, message = "长度不能超过64")
        private String characterName;
        private String description;
        private String characterGoal;
        private Integer isHidden = 0;
        private Integer sortOrder = 0;
    }

    @Data
    public static class StageForm {
        @NotBlank(message = "阶段名称不能为空")
        @Size(max = 64, message = "长度不能超过64")
        private String stageName;
        private String stageDescription;
        @NotNull(message = "阶段顺序不能为空")
        @Min(value = 1, message = "阶段顺序从1开始")
        private Integer stageOrder;
        private String unlockRule;
        private Integer timeoutMinutes;
    }

    @Data
    public static class ClueForm {
        private Long stageId;
        @NotBlank(message = "线索标题不能为空")
        @Size(max = 128, message = "长度不能超过128")
        private String title;
        @NotBlank(message = "线索内容不能为空")
        private String content;
        private String visibilityType = "PUBLIC";
        private Long visibleCharacterId;
        private String eventTime;
        private String location;
        private Integer credibility = 5;
        private Integer isKeyClue = 0;
        private Integer isMisleading = 0;
        private Integer sortOrder = 0;
    }

    @Data
    public static class AnswerForm {
        @NotBlank(message = "人物答案不能为空")
        private String characterAnswer;
        @NotBlank(message = "时间答案不能为空")
        private String timeAnswer;
        @NotBlank(message = "地点答案不能为空")
        private String locationAnswer;
        @NotBlank(message = "行为答案不能为空")
        private String actionAnswer;
        private String scoringRuleJson;
    }

    @Data
    public static class RelationForm {
        @NotNull(message = "线索不能为空")
        private Long clueId;
        @NotBlank(message = "答案字段不能为空")
        private String answerField;
        @NotBlank(message = "关系类型不能为空")
        private String relationType;
        private Integer score = 0;
    }

    @Data
    public static class ClueRef {
        @NotNull(message = "线索编号不能为空")
        private Long clueId;
        private String relationType;
    }

    @Data
    public static class SubmissionRequest {
        @NotBlank(message = "人物答案不能为空")
        private String characterAnswer;
        @NotBlank(message = "时间答案不能为空")
        private String timeAnswer;
        @NotBlank(message = "地点答案不能为空")
        private String locationAnswer;
        @NotBlank(message = "行为答案不能为空")
        private String actionAnswer;
        private String explanation;
        private List<ClueRef> clues;
    }

    @Data
    public static class StatusRequest {
        @NotBlank(message = "状态不能为空")
        private String status;
    }
}
