package com.enigma.mapper;

import com.enigma.entity.GameScript;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GameScriptMapper {

    GameScript selectById(@Param("id") Long id);

    List<GameScript> selectByIds(@Param("ids") List<Long> ids);

    List<GameScript> selectPublished();

    List<GameScript> selectAllOrderByIdDesc();

    int insert(GameScript script);

    int updateById(GameScript script);

    int deleteById(@Param("id") Long id);
}
