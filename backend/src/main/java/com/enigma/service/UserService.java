package com.enigma.service;

import com.enigma.common.BusinessException;
import com.enigma.dto.RequestDtos;
import com.enigma.dto.ViewDtos;
import com.enigma.entity.SysUser;
import com.enigma.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SysUser register(RequestDtos.RegisterRequest request) {
        if (userMapper.countByUsername(request.getUsername()) > 0) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname().trim());
        user.setRole("PLAYER");
        user.setStatus("ACTIVE");
        userMapper.insert(user);
        return user;
    }

    public SysUser getByUsername(String username) {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(401, "登录用户不存在");
        }
        return user;
    }

    public ViewDtos.UserInfo toInfo(SysUser user) {
        return new ViewDtos.UserInfo(user.getId(), user.getUsername(), user.getNickname(),
                user.getAvatar(), user.getBio(), user.getRole(), user.getStatus());
    }

    @Transactional
    public ViewDtos.UserInfo updateProfile(Long userId, RequestDtos.ProfileRequest request) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setNickname(request.getNickname().trim());
        user.setAvatar(request.getAvatar());
        user.setBio(request.getBio());
        userMapper.updateById(user);
        return toInfo(userMapper.selectById(userId));
    }

    @Transactional
    public void changePassword(Long userId, RequestDtos.PasswordRequest request) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("当前密码不正确");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与当前密码相同");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    public List<ViewDtos.UserInfo> listUsers() {
        return userMapper.selectAll()
                .stream().map(this::toInfo).toList();
    }

    @Transactional
    public void updateStatus(Long operatorId, Long userId, String status) {
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) {
            throw new BusinessException("账号状态只能是 ACTIVE 或 DISABLED");
        }
        if (operatorId != null && operatorId.equals(userId) && "DISABLED".equals(status)) {
            throw new BusinessException("不能禁用当前登录的管理员账号");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Transactional
    public void updateRole(Long operatorId, Long userId, String role) {
        if (!"ADMIN".equals(role) && !"PLAYER".equals(role)) {
            throw new BusinessException("角色只能是 ADMIN 或 PLAYER");
        }
        if (operatorId != null && operatorId.equals(userId) && "PLAYER".equals(role)) {
            throw new BusinessException("不能取消当前登录账号的管理员权限");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setRole(role);
        userMapper.updateById(user);
    }
}
