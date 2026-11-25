package com.enigma.mapper;

import com.enigma.entity.GameRoom;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GameRoomMapper {

    GameRoom selectById(@Param("id") Long id);

    List<GameRoom> selectByIds(@Param("ids") List<Long> ids);

    List<GameRoom> selectByScriptId(@Param("scriptId") Long scriptId);

    List<GameRoom> selectRecent(@Param("limit") int limit);

    long countByScriptId(@Param("scriptId") Long scriptId);

    long countByScriptIdAndStatus(@Param("scriptId") Long scriptId, @Param("status") String status);

    int insert(GameRoom room);

    int updateById(GameRoom room);
}
