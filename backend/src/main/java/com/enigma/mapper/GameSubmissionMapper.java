package com.enigma.mapper;

import com.enigma.entity.GameSubmission;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GameSubmissionMapper {

    GameSubmission selectLatestByRoomAndUser(@Param("roomId") Long roomId, @Param("userId") Long userId);

    List<GameSubmission> selectByRoom(@Param("roomId") Long roomId);

    long countByRoom(@Param("roomId") Long roomId);

    long countByRoomAndUser(@Param("roomId") Long roomId, @Param("userId") Long userId);

    int insert(GameSubmission submission);

    int updateById(GameSubmission submission);
}
