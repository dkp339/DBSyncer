package com.rubberhuman.dbsyncer.util;

import com.rubberhuman.dbsyncer.entity.datasource.DataSourceConfig;
import com.rubberhuman.dbsyncer.exception.BusinessException;
import com.rubberhuman.dbsyncer.service.datasource.DataSourceConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

@Component
public class DynamicDbUtil {

    @Autowired
    private DataSourceConfigService dataSourceConfigService;

    @Autowired
    private EncryptionUtil encryptionUtil;

    @Value("${dbsyncer.sync-worker.username}")
    private String syncUsername;

    @Value("${dbsyncer.sync-worker.password}")
    private String syncPassword;

    public JdbcTemplate getJdbcTemplate(Long sourceId) {
        return createJdbcTemplate(sourceId, false);
    }

    public JdbcTemplate getSyncWorkerJdbcTemplate(Long sourceId) {
        return createJdbcTemplate(sourceId, true);
    }

    public JdbcTemplate createJdbcTemplate(Long sourceId, boolean isSync) {
        // 基础校验
        DataSourceConfig config = dataSourceConfigService.getById(sourceId);
        if (config == null) {
            throw new BusinessException("数据源配置不存在: " + sourceId);
        }

        // 提取用户名等明文
        String url = config.getDbType().formatUrl(config.getHost(), config.getPort(), config.getDbName());

        String username;
        String password;

        if (isSync) {
            username = syncUsername;
            password = syncPassword;
        } else {
            username = config.getUsername();

            try {
                password = encryptionUtil.decrypt(config.getPassword());
            } catch (Exception e) {
                throw new RuntimeException("密码解密失败");
            }
        }

        // 创建连接池
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(config.getDbType().getDriverClassName());
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        return new JdbcTemplate(dataSource);
    }
}
