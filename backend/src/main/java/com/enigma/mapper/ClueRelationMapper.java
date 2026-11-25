package com.enigma.mapper;

import com.enigma.entity.ClueRelation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ClueRelationMapper {

    ClueRelation selectById(@Param("id") Long id);

    List<ClueRelation> selectByScriptId(@Param("scriptId") Long scriptId);

    int insert(ClueRelation relation);

    int deleteById(@Param("id") Long id);
}
