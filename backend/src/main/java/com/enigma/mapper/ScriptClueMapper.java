package com.enigma.mapper;

import com.enigma.entity.ScriptClue;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ScriptClueMapper {

    ScriptClue selectById(@Param("id") Long id);

    List<ScriptClue> selectByScriptId(@Param("scriptId") Long scriptId);

    long countByScriptId(@Param("scriptId") Long scriptId);

    int insert(ScriptClue clue);

    int updateById(ScriptClue clue);

    int deleteById(@Param("id") Long id);
}
