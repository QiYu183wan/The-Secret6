package com.enigma.service;

import com.enigma.dto.ViewDtos;
import com.enigma.entity.GameRoom;
import com.enigma.entity.ScriptClue;
import com.enigma.entity.ScriptStage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 线索可见性规则：可见性不能只在前端隐藏，必须由后端过滤。
 */
@Service
@RequiredArgsConstructor
public class ClueAccessService {

    public Map<Long, Integer> stageOrderMap(List<ScriptStage> stages) {
        Map<Long, Integer> map = new HashMap<>();
        for (ScriptStage stage : stages) {
            map.put(stage.getId(), stage.getStageOrder());
        }
        return map;
    }

    public Map<Long, String> stageNameMap(List<ScriptStage> stages) {
        Map<Long, String> map = new HashMap<>();
        for (ScriptStage stage : stages) {
            map.put(stage.getId(), stage.getStageName());
        }
        return map;
    }

    public int currentOrder(GameRoom room, List<ScriptStage> stages) {
        if (room.getCurrentStageId() == null) {
            return stages.isEmpty() ? 1 : stages.get(0).getStageOrder();
        }
        return stageOrderMap(stages).getOrDefault(room.getCurrentStageId(), 1);
    }

    public boolean isVisible(ScriptClue clue, Long characterId,
                             Map<Long, Integer> stageOrders, int currentOrder) {
        int clueOrder = clue.getStageId() == null
                ? 1
                : stageOrders.getOrDefault(clue.getStageId(), 1);
        if (clueOrder > currentOrder) {
            return false;
        }
        String visibility = clue.getVisibilityType() == null ? "PUBLIC" : clue.getVisibilityType();
        return switch (visibility) {
            case "PUBLIC", "HIDDEN" -> true;
            case "CHARACTER" -> characterId != null && characterId.equals(clue.getVisibleCharacterId());
            default -> false;
        };
    }

    public ViewDtos.ClueItem toItem(ScriptClue clue, Map<Long, String> stageNames,
                                    boolean mine, boolean read) {
        return new ViewDtos.ClueItem(clue.getId(), clue.getStageId(),
                clue.getStageId() == null ? null : stageNames.get(clue.getStageId()),
                clue.getTitle(), clue.getContent(), clue.getVisibilityType(),
                clue.getVisibleCharacterId(), clue.getEventTime(), clue.getLocation(),
                clue.getCredibility(), clue.getIsKeyClue(), clue.getIsMisleading(), mine, read);
    }
}
