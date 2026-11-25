package com.enigma.mapper;

import com.enigma.entity.GameResult;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GameResultMapper {

    GameResult selectByRoomAndUser(@Param("roomId") Long roomId, @Param("userId") Long userId);

    List<GameResult> selectByRoom(@Param("roomId") Long roomId);

    List<GameResult> selectByRoomIds(@Param("roomIds") List<Long> roomIds);

    List<GameResult> selectByUserRecent(@Param("userId") Long userId, @Param("limit") int limit);

    long countByRoomAndUser(@Param("roomId") Long roomId, @Param("userId") Long userId);

    int insert(GameResult result);
}
