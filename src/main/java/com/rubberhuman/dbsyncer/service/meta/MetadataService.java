package com.rubberhuman.dbsyncer.service.meta;

import com.rubberhuman.dbsyncer.dto.meta.SqlQueryRequest;
import com.rubberhuman.dbsyncer.entity.datasource.DataSourceConfig;

import java.util.List;
import java.util.Map;

public interface MetadataService {

    List<String> listTables(Long sourceId);

    // 执行任意 SQL 查询
    List<Map<String, Object>> executeSql(SqlQueryRequest request);
}
