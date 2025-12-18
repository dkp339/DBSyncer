package com.rubberhuman.dbsyncer.dto.meta;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class SqlQueryRequest {
    @NotNull(message = "数据源 ID 不能为空")
    private Long sourceId;

    @NotBlank(message = "SQL 语句不能为空")
    private String sql;

    @Min(value = 1, message = "查询条数最小为1")
    @Max(value = 1000, message = "单次查询最多1000条")
    private Integer limit = 100;

    @Min(value = 1, message = "超时时间最小为1秒")
    @Max(value = 30, message = "超时时间最大为30秒")
    private Integer timeoutSeconds = 10;
}
