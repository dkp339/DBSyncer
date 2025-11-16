package com.rubberhuman.dbsyncer.enums.auth;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

public enum AppRole {
    ADMIN("ADMIN", "管理员"),
    USER("USER", "用户");

    @Getter

    // @EnumValue 说明 code 是存入数据库的值
    // @JsonValue 让 Spring MVC 在返回 JSON 时也使用这个值
    @EnumValue
    @JsonValue
    private final String code;

    @Getter
    private final String desc;

    AppRole(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }


}
