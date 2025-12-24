package com.rubberhuman.dbsyncer.service.datasource;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rubberhuman.dbsyncer.entity.datasource.DataSourceConfig;

import java.util.List;

public interface DataSourceConfigService extends IService<DataSourceConfig> {
    // 添加配置
    boolean addDataSource(DataSourceConfig config);

    // 测试连接
    boolean testConnection(DataSourceConfig config);

    // 更新配置
    boolean updateDataSource(DataSourceConfig config);

    // 更新状态
    void updateStatus(Long id, Integer status);

    // 查询所有启用的数据源
    List<DataSourceConfig> listEnabled();

}
