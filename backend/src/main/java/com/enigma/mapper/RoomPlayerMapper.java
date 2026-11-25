package com.enigma.mapper;

import com.enigma.entity.RoomPlayer;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RoomPlayerMapper {

    List<RoomPlayer> selectByRoomId(@Param("roomId") Long roomId);

    List<RoomPlayer> selectByRoomIds(@Param("roomIds") List<Long> roomIds);

    RoomPlayer selectByRoomAndUser(@Param("roomId") Long roomId, @Param("userId") Long userId);

    long countByRoomId(@Param("roomId") Long roomId);

    int insert(RoomPlayer player);
}
