package com.rubberhuman.dbsyncer.mapper.datasource;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rubberhuman.dbsyncer.entity.datasource.DataSourceConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DataSourceConfigMapper extends BaseMapper<DataSourceConfig> {
}
