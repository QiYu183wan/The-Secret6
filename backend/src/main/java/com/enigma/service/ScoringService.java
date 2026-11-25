package com.enigma.service;

import com.enigma.dto.RequestDtos;
import com.enigma.entity.ClueRelation;
import com.enigma.entity.ScriptAnswer;
import com.enigma.entity.ScriptClue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 客观评分：全部依据副本标准答案、评分规则和证据关系计算，结果可复现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringService {

    private static final Pattern FULL_TIME = Pattern.compile(
            "(\\d{4})[-/年](\\d{1,2})[-/月](\\d{1,2})[日\\sT]*(\\d{1,2}):(\\d{2})");
    private static final Pattern CN_TIME = Pattern.compile(
            "(\\d{1,2})月(\\d{1,2})日[^\\d]{0,6}(\\d{1,2}):(\\d{2})");
    private static final Pattern CLOCK = Pattern.compile("(\\d{1,2}):(\\d{2})");

    private final ObjectMapper objectMapper;

    public record Outcome(int score, String result, Map<String, Object> detail) {
    }

    public boolean matchesAnswer(String submitted, String standard) {
        return matches(submitted, standard);
    }

    public String resultOfScore(int score) {
        return resultOf(score);
    }

    public Outcome evaluate(ScriptAnswer answer, RequestDtos.SubmissionRequest request,
                            List<ScriptClue> clues, List<ClueRelation> relations) {
        Map<String, Integer> rules = loadRules(answer.getScoringRuleJson());
        Map<Long, ScriptClue> clueMap = clues.stream()
                .collect(Collectors.toMap(ScriptClue::getId, Function.identity(), (a, b) -> a));

        boolean characterCorrect = matches(request.getCharacterAnswer(), answer.getCharacterAnswer());
        boolean timeCorrect = timeMatches(request.getTimeAnswer(), answer.getTimeAnswer());
        boolean locationCorrect = matches(request.getLocationAnswer(), answer.getLocationAnswer());
        boolean actionCorrect = matches(request.getActionAnswer(), answer.getActionAnswer());

        int characterScore = characterCorrect ? rules.get("characterCorrect") : 0;
        int timeScore = timeCorrect ? rules.get("timeCorrect") : 0;
        int locationScore = locationCorrect ? rules.get("locationCorrect") : 0;
        int actionScore = actionCorrect ? rules.get("actionCorrect") : 0;
        int baseScore = characterScore + timeScore + locationScore + actionScore;

        Set<Long> referenced = request.getClues() == null ? Set.of() : request.getClues().stream()
                .map(RequestDtos.ClueRef::getClueId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, List<ClueRelation>> relationsByClue = relations.stream()
                .collect(Collectors.groupingBy(ClueRelation::getClueId));

        Set<Long> supportIds = new LinkedHashSet<>();
        Set<Long> contradictIds = new LinkedHashSet<>();
        Set<Long> indirectIds = new LinkedHashSet<>();
        Set<Long> keyReferencedIds = new LinkedHashSet<>();
        Set<Long> misleadingIds = new LinkedHashSet<>();
        int supportScore = 0;

        for (Long clueId : referenced) {
            ScriptClue clue = clueMap.get(clueId);
            if (clue == null) {
                continue;
            }
            boolean support = false;
            boolean contradict = false;
            boolean indirect = false;
            boolean key = Integer.valueOf(1).equals(clue.getIsKeyClue());
            boolean misleading = Integer.valueOf(1).equals(clue.getIsMisleading());
            for (ClueRelation relation : relationsByClue.getOrDefault(clueId, List.of())) {
                switch (relation.getRelationType()) {
                    case "SUPPORT" -> {
                        support = true;
                        supportScore += relation.getScore() == null ? 0 : relation.getScore();
                    }
                    case "CONTRADICT" -> contradict = true;
                    case "INDIRECT" -> indirect = true;
                    case "KEY" -> key = true;
                    case "MISLEADING" -> misleading = true;
                    default -> {
                    }
                }
            }
            if (support) {
                supportIds.add(clueId);
            }
            if (contradict) {
                contradictIds.add(clueId);
            }
            if (indirect) {
                indirectIds.add(clueId);
            }
            if (key) {
                keyReferencedIds.add(clueId);
            }
            if (misleading) {
                misleadingIds.add(clueId);
            }
        }
        supportScore = Math.min(supportScore, rules.get("supportClueMax"));

        Set<Long> allKeyIds = new LinkedHashSet<>();
        for (ScriptClue clue : clues) {
            boolean key = Integer.valueOf(1).equals(clue.getIsKeyClue());
            if (!key) {
                key = relationsByClue.getOrDefault(clue.getId(), List.of()).stream()
                        .anyMatch(r -> "KEY".equals(r.getRelationType()));
            }
            if (key) {
                allKeyIds.add(clue.getId());
            }
        }
        Set<Long> missingKeyIds = new LinkedHashSet<>(allKeyIds);
        missingKeyIds.removeAll(referenced);

        boolean timeConflict = hasTimeConflict(request.getTimeAnswer(), answer.getTimeAnswer());
        int keyBonus = keyReferencedIds.isEmpty() ? 0 : rules.get("keyBonus");
        int contradictPenalty = contradictIds.size() * rules.get("contradictPerPenalty");
        int misleadingPenalty = misleadingIds.size() * rules.get("contradictPerPenalty");
        int missingKeyPenalty = missingKeyIds.size() * rules.get("missingKeyPenalty");
        int timeConflictPenalty = timeConflict ? rules.get("timeConflictPenalty") : 0;

        int rawScore = baseScore + supportScore + keyBonus - contradictPenalty
                - misleadingPenalty - missingKeyPenalty - timeConflictPenalty;
        int totalScore = Math.max(0, Math.min(100, rawScore));

        List<String> messages = new ArrayList<>();
        messages.add(characterCorrect ? "人物判断正确 +" + characterScore : "人物判断错误 +0");
        messages.add(timeCorrect ? "时间判断正确 +" + timeScore : "时间判断错误 +0");
        messages.add(locationCorrect ? "地点判断正确 +" + locationScore : "地点判断错误 +0");
        messages.add(actionCorrect ? "行为判断正确 +" + actionScore : "行为判断错误 +0");
        if (supportScore > 0) {
            messages.add("引用支持线索 +" + supportScore);
        }
        if (keyBonus > 0) {
            messages.add("引用关键线索 +" + keyBonus);
        }
        if (contradictPenalty > 0) {
            messages.add("引用矛盾线索 -" + contradictPenalty);
        }
        if (misleadingPenalty > 0) {
            messages.add("引用误导线索 -" + misleadingPenalty);
        }
        if (missingKeyPenalty > 0) {
            messages.add("缺少关键线索 -" + missingKeyPenalty);
        }
        if (timeConflictPenalty > 0) {
            messages.add("存在时间线冲突 -" + timeConflictPenalty);
        }

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("characterCorrect", characterCorrect);
        detail.put("characterScore", characterScore);
        detail.put("timeCorrect", timeCorrect);
        detail.put("timeScore", timeScore);
        detail.put("locationCorrect", locationCorrect);
        detail.put("locationScore", locationScore);
        detail.put("actionCorrect", actionCorrect);
        detail.put("actionScore", actionScore);
        detail.put("baseScore", baseScore);
        detail.put("supportScore", supportScore);
        detail.put("supportClues", titles(supportIds, clueMap));
        detail.put("keyBonus", keyBonus);
        detail.put("keyClues", titles(keyReferencedIds, clueMap));
        detail.put("indirectClues", titles(indirectIds, clueMap));
        detail.put("contradictPenalty", contradictPenalty);
        detail.put("contradictClues", titles(contradictIds, clueMap));
        detail.put("misleadingPenalty", misleadingPenalty);
        detail.put("misleadingClues", titles(misleadingIds, clueMap));
        detail.put("missingKeyPenalty", missingKeyPenalty);
        detail.put("missingKeyClues", titles(missingKeyIds, clueMap));
        detail.put("timeConflict", timeConflict);
        detail.put("timeConflictPenalty", timeConflictPenalty);
        detail.put("rawScore", rawScore);
        detail.put("totalScore", totalScore);
        detail.put("messages", messages);

        return new Outcome(totalScore, resultOf(totalScore), detail);
    }

    private List<String> titles(Set<Long> ids, Map<Long, ScriptClue> clueMap) {
        return ids.stream().map(id -> {
            ScriptClue clue = clueMap.get(id);
            return clue == null ? ("线索#" + id) : clue.getTitle();
        }).toList();
    }

    private String resultOf(int score) {
        if (score >= 80) {
            return "SUCCESS";
        }
        if (score >= 50) {
            return "PARTIAL";
        }
        return "FAIL";
    }

    private Map<String, Integer> loadRules(String json) {
        Map<String, Integer> rules = new HashMap<>();
        rules.put("characterCorrect", 30);
        rules.put("timeCorrect", 20);
        rules.put("locationCorrect", 15);
        rules.put("actionCorrect", 20);
        rules.put("supportClueMax", 10);
        rules.put("keyBonus", 10);
        rules.put("contradictPerPenalty", 5);
        rules.put("missingKeyPenalty", 5);
        rules.put("timeConflictPenalty", 10);
        if (json == null || json.isBlank()) {
            return rules;
        }
        try {
            Map<String, Integer> parsed = objectMapper.readValue(json,
                    new TypeReference<Map<String, Integer>>() {
                    });
            parsed.forEach((key, value) -> {
                if (value != null) {
                    rules.put(key, value);
                }
            });
        } catch (Exception e) {
            log.warn("评分规则解析失败，使用默认规则：{}", e.getMessage());
        }
        return rules;
    }

    private boolean matches(String submitted, String standard) {
        if (submitted == null || standard == null) {
            return false;
        }
        String a = normalize(submitted);
        String b = normalize(standard);
        return !a.isEmpty() && (a.equals(b) || a.contains(b) || b.contains(a));
    }

    private boolean timeMatches(String submitted, String standard) {
        if (submitted == null || standard == null) {
            return false;
        }
        int[] a = clockOf(submitted);
        int[] b = clockOf(standard);
        if (a != null && b != null) {
            return a[0] == b[0] && a[1] == b[1];
        }
        return matches(submitted, standard);
    }

    private boolean hasTimeConflict(String submitted, String standard) {
        LocalDateTime submittedTime = parseDateTime(submitted, standard);
        LocalDateTime standardTime = parseDateTime(standard, standard);
        if (submittedTime == null || standardTime == null) {
            return false;
        }
        if (submittedTime.toLocalDate().equals(standardTime.toLocalDate())
                && submittedTime.toLocalTime().equals(standardTime.toLocalTime())) {
            return false;
        }
        return submittedTime.isBefore(standardTime);
    }

    private LocalDateTime parseDateTime(String text, String reference) {
        if (text == null) {
            return null;
        }
        Matcher full = FULL_TIME.matcher(text);
        if (full.find()) {
            return LocalDateTime.of(Integer.parseInt(full.group(1)), Integer.parseInt(full.group(2)),
                    Integer.parseInt(full.group(3)), Integer.parseInt(full.group(4)),
                    Integer.parseInt(full.group(5)));
        }
        Matcher cn = CN_TIME.matcher(text);
        if (cn.find()) {
            int year = 2024;
            if (reference != null) {
                Matcher ref = FULL_TIME.matcher(reference);
                if (ref.find()) {
                    year = Integer.parseInt(ref.group(1));
                }
            }
            return LocalDateTime.of(year, Integer.parseInt(cn.group(1)), Integer.parseInt(cn.group(2)),
                    Integer.parseInt(cn.group(3)), Integer.parseInt(cn.group(4)));
        }
        return null;
    }

    private int[] clockOf(String text) {
        Matcher matcher = CLOCK.matcher(text);
        if (matcher.find()) {
            return new int[]{Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))};
        }
        return null;
    }

    private String normalize(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s，。、；：,.;:!?！？\"“”'']", "");
    }
}
