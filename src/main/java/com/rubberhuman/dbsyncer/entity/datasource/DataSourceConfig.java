package com.rubberhuman.dbsyncer.entity.datasource;

import com.baomidou.mybatisplus.annotation.*;
import com.rubberhuman.dbsyncer.enums.datasource.DatabaseType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("data_source_config")
public class DataSourceConfig {

    @TableId(value = "source_id",type = IdType.AUTO)
    private Long sourceId;

    // 连接名称（给用户看的，比如 "主生产库-MySQL"）
    private String sourceName;

    // 数据库类型：MYSQL, ORACLE, POSTGRESQL
    private DatabaseType dbType;

    // 主机地址 (IP)
    private String host;

    // 端口号 (MySQL默认3306, Oracle 1521, PostgreSQL 5432)
    private String port;

    // 数据库名 (MySQL) 或 SID/ServiceName (Oracle)
    private String dbName;

    // 用户名
    private String username;

    // 密码，数据库内加密
    @TableField("password_encrypted")
    private String password;

    // 状态 (1: 启用, 0: 禁用)
    private Integer status;

    // 逻辑删除
    @TableLogic
    private Integer deleted;

    // 创建/更新时间
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
