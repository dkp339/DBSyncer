package com.rubberhuman.dbsyncer.controller.auth;

import com.rubberhuman.dbsyncer.DTO.auth.LoginRequest;
import com.rubberhuman.dbsyncer.DTO.auth.LoginResponse;
import com.rubberhuman.dbsyncer.util.JwtTokenUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) throws Exception {
        try {
            // 1. 使用 Spring Security 的 AuthenticationManager 进行认证
            // 这一步会调用 AppUserDetailsService 和 PasswordEncoder
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
        } catch (BadCredentialsException e) {
            // 2. 认证失败 (密码错误)
            return ResponseEntity.status(401).body(Map.of("error", "用户名或密码错误"));
        }

        // 3. 认证成功，加载 UserDetails
        final UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getUsername());

        // 4. 生成 JWT
        final String token = jwtTokenUtil.generateToken(userDetails);

        // 5. 返回 Token
        return ResponseEntity.ok(new LoginResponse(token));
    }
}
