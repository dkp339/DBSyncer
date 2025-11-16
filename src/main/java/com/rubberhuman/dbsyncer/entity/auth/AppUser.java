package com.rubberhuman.dbsyncer.entity.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rubberhuman.dbsyncer.enums.auth.AppRole;
import lombok.Data;
import java.sql.Timestamp;

@Data
@TableName("app_users") // 对应您的元数据表
public class AppUser {

    @TableId(value = "user_id", type = IdType.AUTO)
    private Integer userId;

    private String username;

    @TableField("password_hash")
    private String password;

    private AppRole role; // "ADMIN" 或 "USER"
    private Timestamp createdAt;
}

