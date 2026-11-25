package com.enigma.mapper;

import com.enigma.entity.SysUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysUserMapper {

    SysUser selectById(@Param("id") Long id);

    SysUser selectByUsername(@Param("username") String username);

    long countByUsername(@Param("username") String username);

    List<SysUser> selectAll();

    List<SysUser> selectByIds(@Param("ids") List<Long> ids);

    int insert(SysUser user);

    int updateById(SysUser user);
}
