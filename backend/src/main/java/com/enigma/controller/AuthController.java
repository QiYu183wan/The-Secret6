package com.enigma.controller;

import com.enigma.common.R;
import com.enigma.dto.RequestDtos;
import com.enigma.dto.ViewDtos;
import com.enigma.entity.SysUser;
import com.enigma.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    @PostMapping("/api/register")
    public R<ViewDtos.UserInfo> register(@Valid @RequestBody RequestDtos.RegisterRequest request,
                                        HttpServletRequest httpRequest,
                                        HttpServletResponse httpResponse) {
        SysUser user = userService.register(request);
        authenticate(request.getUsername(), request.getPassword(), httpRequest, httpResponse);
        return R.ok(userService.toInfo(user));
    }

    @PostMapping("/api/login")
    public R<ViewDtos.UserInfo> login(@Valid @RequestBody RequestDtos.LoginRequest request,
                                     HttpServletRequest httpRequest,
                                     HttpServletResponse httpResponse) {
        Authentication authentication = authenticate(request.getUsername(), request.getPassword(),
                httpRequest, httpResponse);
        return R.ok(userService.toInfo(userService.getByUsername(authentication.getName())));
    }

    @PostMapping("/api/logout")
    public R<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        SecurityContextHolder.clearContext();
        return R.ok();
    }

    private Authentication authenticate(String username, String password,
                                        HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        request.getSession(true);
        return authentication;
    }
}
