package com.enigma.security;

import com.enigma.common.BusinessException;
import com.enigma.entity.SysUser;
import com.enigma.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 获取当前登录用户，登录态由 Spring Security Session 维护。
 */
@Component
@RequiredArgsConstructor
public class CurrentUserService {

    private final SysUserMapper userMapper;

    public SysUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException(401, "请先登录");
        }
        SysUser user = userMapper.selectByUsername(authentication.getName());
        if (user == null) {
            throw new BusinessException(401, "登录用户不存在");
        }
        return user;
    }

    public Long id() {
        return get().getId();
    }
}
