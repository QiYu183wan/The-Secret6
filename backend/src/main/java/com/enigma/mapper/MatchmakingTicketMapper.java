package com.enigma.mapper;

import com.enigma.entity.MatchmakingTicket;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MatchmakingTicketMapper {

    MatchmakingTicket selectWaiting(@Param("userId") Long userId, @Param("scriptId") Long scriptId);

    MatchmakingTicket selectMatched(@Param("userId") Long userId, @Param("scriptId") Long scriptId);

    MatchmakingTicket selectLatest(@Param("userId") Long userId, @Param("scriptId") Long scriptId);

    List<MatchmakingTicket> selectWaitingList(@Param("scriptId") Long scriptId,
                                             @Param("limit") int limit);

    long countWaiting(@Param("scriptId") Long scriptId);

    int insert(MatchmakingTicket ticket);

    int updateById(MatchmakingTicket ticket);

    int markMatched(@Param("id") Long id, @Param("roomId") Long roomId);
}
