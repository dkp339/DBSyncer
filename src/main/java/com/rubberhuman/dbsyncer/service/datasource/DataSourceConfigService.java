package com.rubberhuman.dbsyncer.service.datasource;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rubberhuman.dbsyncer.entity.datasource.DataSourceConfig;

public interface DataSourceConfigService extends IService<DataSourceConfig> {
    // 添加配置（包含密码加密逻辑）
    boolean addDataSource(DataSourceConfig config);

    // 测试连接（核心业务）
    boolean testConnection(DataSourceConfig config);

    // 获取解密后的真实配置（供同步引擎内部使用）
    DataSourceConfig getDecryptedConfig(Long id);
}
