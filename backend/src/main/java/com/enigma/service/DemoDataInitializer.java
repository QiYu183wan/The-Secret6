package com.enigma.service;

import com.enigma.entity.SysUser;
import com.enigma.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 首次启动时写入演示账号，密码使用 PasswordEncoder 加密。
 * 演示账号：admin / admin123，player1 ~ player4 / 123456
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        createIfAbsent("admin", "admin123", "管理员", "ADMIN");
        createIfAbsent("player1", "123456", "玩家一号", "PLAYER");
        createIfAbsent("player2", "123456", "玩家二号", "PLAYER");
        createIfAbsent("player3", "123456", "玩家三号", "PLAYER");
        createIfAbsent("player4", "123456", "玩家四号", "PLAYER");
    }

    private void createIfAbsent(String username, String rawPassword, String nickname, String role) {
        if (userMapper.countByUsername(username) > 0) {
            return;
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setNickname(nickname);
        user.setRole(role);
        user.setStatus("ACTIVE");
        userMapper.insert(user);
        log.info("已创建演示账号：{} / {}", username, rawPassword);
    }
}
