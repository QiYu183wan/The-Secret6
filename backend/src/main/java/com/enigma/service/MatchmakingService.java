package com.enigma.service;

import com.enigma.common.BusinessException;
import com.enigma.dto.ViewDtos;
import com.enigma.entity.GameRoom;
import com.enigma.entity.GameScript;
import com.enigma.entity.MatchmakingTicket;
import com.enigma.entity.PlayerClue;
import com.enigma.entity.RoomPlayer;
import com.enigma.entity.RoomStageRecord;
import com.enigma.entity.ScriptCharacter;
import com.enigma.entity.ScriptClue;
import com.enigma.entity.ScriptStage;
import com.enigma.mapper.GameRoomMapper;
import com.enigma.mapper.GameScriptMapper;
import com.enigma.mapper.MatchmakingTicketMapper;
import com.enigma.mapper.PlayerClueMapper;
import com.enigma.mapper.RoomPlayerMapper;
import com.enigma.mapper.RoomStageRecordMapper;
import com.enigma.mapper.ScriptCharacterMapper;
import com.enigma.mapper.ScriptClueMapper;
import com.enigma.mapper.ScriptStageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MatchmakingService {

    private final GameScriptMapper scriptMapper;
    private final MatchmakingTicketMapper ticketMapper;
    private final ScriptCharacterMapper characterMapper;
    private final ScriptStageMapper stageMapper;
    private final ScriptClueMapper clueMapper;
    private final GameRoomMapper roomMapper;
    private final RoomPlayerMapper roomPlayerMapper;
    private final PlayerClueMapper playerClueMapper;
    private final RoomStageRecordMapper stageRecordMapper;
    private final ClueAccessService clueAccess;

    private final ConcurrentHashMap<Long, Object> locks = new ConcurrentHashMap<>();

    @Transactional
    public ViewDtos.MatchStatus join(Long scriptId, Long userId) {
        GameScript script = requireScript(scriptId);
        if (!"PUBLISHED".equals(script.getStatus())) {
            throw new BusinessException("副本未发布或已下架，无法加入匹配");
        }
        synchronized (locks.computeIfAbsent(scriptId, key -> new Object())) {
            MatchmakingTicket waiting = ticketMapper.selectWaiting(userId, scriptId);
            if (waiting == null) {
                MatchmakingTicket matched = ticketMapper.selectMatched(userId, scriptId);
                if (matched != null && matched.getMatchedRoomId() != null) {
                    GameRoom room = roomMapper.selectById(matched.getMatchedRoomId());
                    if (room != null && !"FINISHED".equals(room.getStatus())
                            && !"ABANDONED".equals(room.getStatus())) {
                        return buildStatus(script, "MATCHED", room.getId(), null);
                    }
                }
                MatchmakingTicket ticket = new MatchmakingTicket();
                ticket.setUserId(userId);
                ticket.setScriptId(scriptId);
                ticket.setStatus("WAITING");
                ticketMapper.insert(ticket);
            }
            tryCreateRoom(script);
            MatchmakingTicket latest = ticketMapper.selectLatest(userId, scriptId);
            return buildStatus(script, latest.getStatus(), latest.getMatchedRoomId(), latest.getCreateTime());
        }
    }

    public ViewDtos.MatchStatus status(Long scriptId, Long userId) {
        GameScript script = requireScript(scriptId);
        MatchmakingTicket latest = ticketMapper.selectLatest(userId, scriptId);
        if (latest == null) {
            return buildStatus(script, "NONE", null, null);
        }
        return buildStatus(script, latest.getStatus(), latest.getMatchedRoomId(), latest.getCreateTime());
    }

    @Transactional
    public void cancel(Long scriptId, Long userId) {
        MatchmakingTicket waiting = ticketMapper.selectWaiting(userId, scriptId);
        if (waiting == null) {
            throw new BusinessException("当前没有等待中的匹配记录");
        }
        waiting.setStatus("CANCELLED");
        ticketMapper.updateById(waiting);
    }

    private GameScript requireScript(Long scriptId) {
        GameScript script = scriptMapper.selectById(scriptId);
        if (script == null) {
            throw new BusinessException(404, "副本不存在");
        }
        return script;
    }

    private ViewDtos.MatchStatus buildStatus(GameScript script, String status, Long roomId,
                                             LocalDateTime createTime) {
        long waiting = ticketMapper.countWaiting(script.getId());
        return new ViewDtos.MatchStatus(script.getId(), status, roomId, (int) waiting,
                script.getRequiredPlayers(), createTime);
    }

    private void tryCreateRoom(GameScript script) {
        int required = script.getRequiredPlayers();
        List<MatchmakingTicket> waiting = ticketMapper.selectWaitingList(script.getId(), required);
        if (waiting.size() < required) {
            return;
        }
        List<ScriptCharacter> characters = new ArrayList<>(characterMapper.selectByScriptId(script.getId()));
        if (characters.size() < required) {
            throw new BusinessException("副本角色数量不足，无法开局");
        }
        List<ScriptStage> stages = stageMapper.selectByScriptId(script.getId());
        if (stages.isEmpty()) {
            throw new BusinessException("副本尚未配置阶段，无法开局");
        }
        Collections.shuffle(characters);
        ScriptStage firstStage = stages.get(0);

        GameRoom room = new GameRoom();
        room.setScriptId(script.getId());
        room.setCurrentStageId(firstStage.getId());
        room.setStatus("IN_PROGRESS");
        room.setStartTime(LocalDateTime.now());
        roomMapper.insert(room);

        for (int i = 0; i < required; i++) {
            MatchmakingTicket ticket = waiting.get(i);
            RoomPlayer player = new RoomPlayer();
            player.setRoomId(room.getId());
            player.setUserId(ticket.getUserId());
            player.setCharacterId(characters.get(i).getId());
            player.setStatus("ACTIVE");
            roomPlayerMapper.insert(player);

            int updated = ticketMapper.markMatched(ticket.getId(), room.getId());
            if (updated == 0) {
                throw new BusinessException("匹配状态已变化，请重新加入匹配");
            }
        }

        RoomStageRecord record = new RoomStageRecord();
        record.setRoomId(room.getId());
        record.setStageId(firstStage.getId());
        record.setUnlockReason("匹配成功，副本开始");
        record.setIsOpen(1);
        stageRecordMapper.insert(record);

        distributeClues(room, stages, firstStage.getStageOrder());
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
}
