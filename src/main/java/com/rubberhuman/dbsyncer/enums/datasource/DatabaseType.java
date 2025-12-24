package com.rubberhuman.dbsyncer.enums.datasource;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DatabaseType {
    /**
     * MySQL 数据库
     * 默认端口: 3306
     * URL 示例: jdbc:mysql://127.0.0.1:3306/dbname?useSSL=false...
     */
    MYSQL("MYSQL", "MySQL数据库", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=UTC&characterEncoding=utf-8&allowPublicKeyRetrieval=true"),

    /**
     * Oracle 数据库
     * 默认端口: 1521
     * URL 示例 (Thin模式): jdbc:oracle:thin:@127.0.0.1:1521:ORCL
     */
    ORACLE("ORACLE", "Oracle数据库", "oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@//%s:%s/%s"),

    /**
     * PostgreSQL 数据库
     * 默认端口: 5432
     * URL 示例: jdbc:postgresql://127.0.0.1:5432/dbname
     */
    POSTGRESQL("POSTGRESQL", "PostgreSQL数据库", "org.postgresql.Driver", "jdbc:postgresql://%s:%s/%s"),

    /**
     * SQL Server 数据库
     * 默认端口: 1433
     * URL 示例: jdbc:sqlserver://127.0.0.1:1433;DatabaseName=dbname...
     */
    SQL_SERVER("SQL_SERVER", "SQL Server数据库", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://%s:%s;DatabaseName=%s;encrypt=false");


    @EnumValue
    // 存入数据库的值 (例如 "MYSQL")
    private final String code;

    // 前端展示的描述 (例如 "MySQL数据库")
    private final String description;

    // JDBC 驱动类名
    private final String driverClassName;

    // JDBC URL 模板 (%s 占位符分别对应 host, port, dbName)
    private final String urlPattern;

    /**
     * 生成最终的 JDBC URL
     *
     * @param host   主机IP
     * @param port   端口
     * @param dbName 数据库名/SID
     * @return 完整的 JDBC URL
     */
    public String formatUrl(String host, String port, String dbName) {
        return String.format(this.urlPattern, host, port, dbName);
    }

    public static DatabaseType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (DatabaseType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的数据库类型 code: " + code);
    }
}
