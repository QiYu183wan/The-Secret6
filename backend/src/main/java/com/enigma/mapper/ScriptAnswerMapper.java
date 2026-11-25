package com.enigma.mapper;

import com.enigma.entity.ScriptAnswer;
import org.apache.ibatis.annotations.Param;

public interface ScriptAnswerMapper {

    ScriptAnswer selectByScriptId(@Param("scriptId") Long scriptId);

    long countByScriptId(@Param("scriptId") Long scriptId);

    int insert(ScriptAnswer answer);

    int updateById(ScriptAnswer answer);
}
