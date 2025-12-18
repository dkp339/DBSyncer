package com.rubberhuman.dbsyncer.dto.auth;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
