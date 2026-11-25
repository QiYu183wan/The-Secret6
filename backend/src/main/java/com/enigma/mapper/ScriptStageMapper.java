package com.enigma.mapper;

import com.enigma.entity.ScriptStage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ScriptStageMapper {

    ScriptStage selectById(@Param("id") Long id);

    List<ScriptStage> selectByScriptId(@Param("scriptId") Long scriptId);

    int insert(ScriptStage stage);

    int updateById(ScriptStage stage);

    int deleteById(@Param("id") Long id);
}
