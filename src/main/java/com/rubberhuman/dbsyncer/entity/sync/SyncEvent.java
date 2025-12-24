package com.rubberhuman.dbsyncer.entity.sync;

import com.rubberhuman.dbsyncer.enums.datasource.DatabaseType;
import lombok.Data;

import java.util.Date;

@Data
public class SyncEvent {
    private Long id;
    private String tableName; // 表名
    private String opType;    // INSERT, UPDATE, DELETE
    private String pkColumnName;
    private String pkValue;   // 主键值
    private Integer status;   // 0:未处理, 1:成功, 2:失败
    private Date opTime;      // 发生时间
    private DatabaseType sourceDbType;
    private String errorMsg;  // 错误信息
    private Long dataVersion;
}
