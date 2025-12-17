package com.rubberhuman.dbsyncer.service.datasource;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rubberhuman.dbsyncer.entity.datasource.DataSourceConfig;

public interface DataSourceConfigService extends IService<DataSourceConfig> {
    // 添加配置
    boolean addDataSource(DataSourceConfig config);

    // 测试连接
    boolean testConnection(DataSourceConfig config);

    // 更新配置
    boolean updateDataSource(DataSourceConfig config);

    // 更新状态
    void updateStatus(Long id, Integer status);

    // 获取解密后的真实配置
    DataSourceConfig getDecryptedConfig(Long id);
}
