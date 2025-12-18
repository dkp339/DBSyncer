package com.rubberhuman.dbsyncer.security;

import com.rubberhuman.dbsyncer.exception.BusinessException;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class SqlSecurityManager {

    private static final Set<String> FORBIDDEN_TABLES = new HashSet<>(Arrays.asList(
            "information_schema", "mysql", "performance_schema", "sys", // MySQL
            "pg_catalog", "pg_toast", // PostgreSQL
            "user_users", "all_users", "dba_users" // Oracle
    ));

    public void check(String sql) {
        // 语法解析
        Statement statement;
        try {
            statement = CCJSqlParserUtil.parse(sql);
        } catch (JSQLParserException e) {
            throw new BusinessException("SQL 语法错误: " + e.getMessage());
        }

        // 限制只能执行 SELECT
        if (!(statement instanceof Select)) {
            throw new BusinessException("控制台仅允许执行 SELECT 查询语句");
        }

        // 提取所有涉及的表名，包括子查询、JOIN、UNION 中的表
        TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
        List<String> tableList = tablesNamesFinder.getTableList(statement);

        // 黑名单检查
        for (String tableName : tableList) {
            // 移除可能存在的反引号或引号 (e.g., `mysql`.`user` -> mysql.user)
            String cleanName = tableName.replace("`", "").replace("\"", "").toLowerCase();

            // 检查是否包含敏感关键字
            if (isForbidden(cleanName)) {
                throw new BusinessException("禁止查询系统敏感表: " + tableName);
            }
        }
    }

    // 辅助函数：查看是否在黑名单内
    private boolean isForbidden(String tableName) {
        String lowerName = tableName.toLowerCase();

        // 防止此类含有点号的表名被误杀
        if (lowerName.contains(".")) {

            String[] parts = lowerName.split("\\.");
            for (String part : parts) {
                if (FORBIDDEN_TABLES.contains(part)) {
                    return true;
                }
            }
        }
        else {
            if (FORBIDDEN_TABLES.contains(lowerName)) {
                return true;
            }
        }

        return false;
    }
}
