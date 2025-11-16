package com.rubberhuman.dbsyncer.DTO.auth;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private long expiresIn; // 秒
}
