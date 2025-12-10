package com.rubberhuman.dbsyncer.service.datasource;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rubberhuman.dbsyncer.entity.datasource.DataSourceConfig;
import com.rubberhuman.dbsyncer.mapper.datasource.DataSourceConfigMapper;
import com.rubberhuman.dbsyncer.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Service
public class DataSourceConfigServiceImpl extends ServiceImpl<DataSourceConfigMapper, DataSourceConfig> implements DataSourceConfigService {

    @Autowired
    private EncryptionUtil encryptionUtil;

    @Override
    public boolean addDataSource(DataSourceConfig config) {
        // 1. 校验必填项
        // 2. 加密密码
        String encryptedPwd = encryptionUtil.encrypt(config.getPassword());
        config.setPassword(encryptedPwd);
        return this.save(config);
    }


    // 测试连接：不依赖 MyBatis，直接用原生 JDBC 尝试连接
    @Override
    public boolean testConnection(DataSourceConfig config) {
        // 1. 直接从枚举中获取 Driver 类名
        String driverClass = config.getDbType().getDriverClassName();

        // 2. 直接调用枚举的方法生成 URL
        // 不需要任何 if-else 判断，枚举自己知道怎么拼 URL
        String url = config.getDbType().formatUrl(
                config.getHost(),
                config.getPort(),
                config.getDbName()
        );

        Connection conn = null;
        try {
            Class.forName(driverClass); // 加载驱动
            conn = DriverManager.getConnection(url, config.getUsername(), config.getPassword());
            return conn != null;
        } catch (Exception e) {
            log.error("连接测试失败", e);
            throw new RuntimeException("连接失败: " + e.getMessage());
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }

    @Override
    public DataSourceConfig getDecryptedConfig(Long id) {
        // 1. 从数据库查询原始记录，此时 password 是密文
        DataSourceConfig config = this.getById(id);

        // 2. 判空处理
        if (config == null) {
            return null;
        }

        // 3. 获取密文密码
        String encryptedPwd = config.getPassword();

        // 4. 解密逻辑
        if (encryptedPwd != null && !encryptedPwd.isEmpty()) {
            try {
                String plainPwd = encryptionUtil.decrypt(encryptedPwd);

                // 将明文密码设置回对象中，只存在于内存中
                config.setPassword(plainPwd);

            } catch (Exception e) {
                log.error("数据源ID=" + id + " 密码解密失败", e);
                throw new RuntimeException("数据源配置异常：密码解密失败，请确认密钥配置一致", e);
            }
        }

        return config;
    }

}
