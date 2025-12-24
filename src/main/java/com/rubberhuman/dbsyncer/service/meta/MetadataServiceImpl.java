package com.rubberhuman.dbsyncer.service.meta;

import com.rubberhuman.dbsyncer.dto.meta.SqlQueryRequest;
import com.rubberhuman.dbsyncer.entity.datasource.DataSourceConfig;
import com.rubberhuman.dbsyncer.exception.BusinessException;
import com.rubberhuman.dbsyncer.security.SqlSecurityManager;
import com.rubberhuman.dbsyncer.service.datasource.DataSourceConfigService;
import com.rubberhuman.dbsyncer.util.DynamicDbUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MetadataServiceImpl implements MetadataService {

    @Autowired
    private DynamicDbUtil dynamicDbUtil;

    @Autowired
    private DataSourceConfigService configService;

    @Autowired
    private SqlSecurityManager sqlSecurityManager;

    // 获取指定数据源下的所有表名
    @Override
    public List<String> listTables(Long sourceId) {
        JdbcTemplate jdbcTemplate = dynamicDbUtil.getJdbcTemplate(sourceId);

        DataSourceConfig config = configService.getById(sourceId);

        String sql = getTableListSql(config);

        try {
            List<String> tables = jdbcTemplate.queryForList(sql, String.class);
            return tables.stream()
                    .filter(tableName -> !tableName.equalsIgnoreCase("sync_event"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new BusinessException("获取表列表失败: " + e.getMessage());
        }
    }

    // 执行任意 SQL 查询
    @Override
    public List<Map<String, Object>> executeSql(SqlQueryRequest request) {
        Long sourceId = request.getSourceId();
        String sql = request.getSql();

        // 安全检查
        sqlSecurityManager.check(sql);

        JdbcTemplate jdbcTemplate = dynamicDbUtil.getJdbcTemplate(sourceId);

        int maxRows = (request.getLimit() != null) ? request.getLimit() : 100;
        jdbcTemplate.setMaxRows(maxRows);

        int timeout = (request.getTimeoutSeconds() != null) ? request.getTimeoutSeconds() : 10;
        jdbcTemplate.setQueryTimeout(timeout);

        String upperSql = sql.toUpperCase();
        try {
            // 查询语句
            if (upperSql.startsWith("SELECT") || upperSql.startsWith("SHOW") || upperSql.startsWith("DESC")) {
                return jdbcTemplate.queryForList(sql);
            }
            // 增删改语句，返回影响行数 (int)
            else {
                int rows = jdbcTemplate.update(sql);
                Map<String, Object> result = new HashMap<>();
                result.put("affected_rows", rows);
                result.put("message", "执行成功");
                return Collections.singletonList(result);
            }

        } catch (Exception e) {
            throw new BusinessException("SQL 执行错误: " + e.getCause().getMessage());
        }
    }

    // 分析优化
    @Override
    public List<Map<String, Object>> explainSql(SqlQueryRequest request) {

        String sql = request.getSql().trim();
        if (!sql.toUpperCase().startsWith("SELECT")) {
            throw new BusinessException("只有 SELECT 语句支持分析优化");
        }

        String explainSql = "EXPLAIN " + sql;

        JdbcTemplate jdbcTemplate = dynamicDbUtil.getJdbcTemplate(request.getSourceId());
        return jdbcTemplate.queryForList(explainSql);
    }

    // 辅助方法：生成不同数据库系统下的获取所有表名的方法
    private String getTableListSql(DataSourceConfig config) {
        switch (config.getDbType()) {
            case MYSQL:
                return "SHOW TABLES";
            case ORACLE:
                return "SELECT TABLE_NAME FROM USER_TABLES";
            case SQL_SERVER:
                return "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE'";
            case POSTGRESQL:
                return "SELECT tablename FROM pg_tables WHERE schemaname='public'";
            default:
                throw new BusinessException("不支持的数据库类型");
        }
    }
}
