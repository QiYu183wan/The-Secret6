package com.enigma.mapper;

import com.enigma.entity.PlayerClue;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PlayerClueMapper {

    List<PlayerClue> selectByRoomAndUser(@Param("roomId") Long roomId, @Param("userId") Long userId);

    long countByRoomUserClue(@Param("roomId") Long roomId, @Param("userId") Long userId,
                             @Param("clueId") Long clueId);

    int insert(PlayerClue playerClue);

    int markAllRead(@Param("roomId") Long roomId, @Param("userId") Long userId);
}
