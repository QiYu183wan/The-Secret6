package com.enigma.mapper;

import com.enigma.entity.ScriptCharacter;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ScriptCharacterMapper {

    ScriptCharacter selectById(@Param("id") Long id);

    List<ScriptCharacter> selectByScriptId(@Param("scriptId") Long scriptId);

    List<ScriptCharacter> selectByIds(@Param("ids") List<Long> ids);

    long countByScriptId(@Param("scriptId") Long scriptId);

    int insert(ScriptCharacter character);

    int updateById(ScriptCharacter character);

    int deleteById(@Param("id") Long id);
}
