package com.enigma.mapper;

import com.enigma.entity.RoomStageRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RoomStageRecordMapper {

    List<RoomStageRecord> selectByRoom(@Param("roomId") Long roomId);

    RoomStageRecord selectByRoomAndStage(@Param("roomId") Long roomId, @Param("stageId") Long stageId);

    int insert(RoomStageRecord record);

    int updateById(RoomStageRecord record);
}
