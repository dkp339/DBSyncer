package com.rubberhuman.dbsyncer.service.datasource;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rubberhuman.dbsyncer.entity.datasource.DataSourceConfig;
import com.rubberhuman.dbsyncer.exception.BusinessException;
import com.rubberhuman.dbsyncer.mapper.datasource.DataSourceConfigMapper;
import com.rubberhuman.dbsyncer.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Service
public class DataSourceConfigServiceImpl extends ServiceImpl<DataSourceConfigMapper, DataSourceConfig> implements DataSourceConfigService {

    @Autowired
    private EncryptionUtil encryptionUtil;

    @Override
    public boolean addDataSource(DataSourceConfig config) {

        // 非空校验
        Assert.hasText(config.getSourceName(), "数据源名称不能为空");
        Assert.notNull(config.getDbType(), "数据库类型不能为空");
        Assert.hasText(config.getHost(), "主机地址不能为空");
        Assert.notNull(config.getPort(), "端口不能为空");
        Assert.hasText(config.getDbName(), "数据库名不能为空");
        Assert.hasText(config.getUsername(), "用户名不能为空");
        Assert.hasText(config.getPassword(), "密码不能为空");

        // sourceName 需要 unique 检验（数据库内有约束）
        boolean exists = lambdaQuery()
                .eq(DataSourceConfig::getSourceName, config.getSourceName())
                .exists();
        if (exists) {
            throw new BusinessException("数据源名称已存在");
        }

        // config 实体内的 password 是原始密码，保存在数据库内需要加密
        String encryptedPwd = encryptionUtil.encrypt(config.getPassword());
        config.setPassword(encryptedPwd);

        // 再次为 unique 字段兜底
        try {
            return this.save(config);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("数据源名称已存在");
        }
    }


    // 测试连接：不依赖 MyBatis，用原生 JDBC 尝试连接
    @Override
    public boolean testConnection(DataSourceConfig config) {
        // 从枚举中获取 Driver 类名
        String driverClass = config.getDbType().getDriverClassName();

        // 生成 URL
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
            throw new BusinessException("连接失败: " + e.getMessage());
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }

    @Override
    public boolean updateDataSource(DataSourceConfig config) {
        if (config.getSourceId() == null) {
            throw new BusinessException("数据源 ID 不能为空");
        }

        if (!hasAnyUpdateField(config)) {
            throw new BusinessException("请输入需要修改的项");
        }

        // 查询旧数据
        DataSourceConfig old = this.getById(config.getSourceId());
        if (old == null) {
            throw new BusinessException("数据源不存在");
        }

        mergeConfig(config, old);
        return this.updateById(old);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        // 参数校验
        if (id == null || status == null) {
            throw new BusinessException("参数缺失");
        }
        // 检查状态值是否合法
        if (status != 0 && status != 1) {
            throw new BusinessException("状态值不合法");
        }

        boolean success = this.lambdaUpdate()
                .eq(DataSourceConfig::getSourceId, id)
                .set(DataSourceConfig::getStatus, status)
                .update();

        if (!success) {
            throw new BusinessException("更新失败，数据源可能不存在");
        }
    }

    @Override
    public DataSourceConfig getDecryptedConfig(Long id) {
        // 数据库内 password 是密文
        DataSourceConfig config = this.getById(id);

        if (config == null) {
            return null;
        }

        String encryptedPwd = config.getPassword();

        // 解密
        if (encryptedPwd != null && !encryptedPwd.isEmpty()) {
            try {
                String plainPwd = encryptionUtil.decrypt(encryptedPwd);

                // 将明文密码设置回 config 实体对象中
                config.setPassword(plainPwd);

            } catch (Exception e) {
                log.error("数据源ID=" + id + " 密码解密失败", e);
                throw new BusinessException("数据源配置异常：密码解密失败，请确认密钥配置一致", e);
            }
        }

        return config;
    }


    // ******** update 相关辅助方法 ********
    // 校验是否真的有修改内容
    private boolean hasAnyUpdateField(DataSourceConfig config) {
        return config.getSourceName() != null
                || config.getDbType() != null
                || config.getHost() != null
                || config.getPort() != null
                || config.getDbName() != null
                || config.getUsername() != null
                || config.getPassword() != null
                || config.getStatus() != null;
    }

    // 判断字符串不为 null 且不为空
    private boolean hasText(String str) {
        return str != null && !str.trim().isEmpty();
    }

    // 合并数据
    private void mergeConfig(DataSourceConfig src, DataSourceConfig target) {

        if (hasText(src.getSourceName())) target.setSourceName(src.getSourceName());
        if (src.getDbType() != null) target.setDbType(src.getDbType());
        if (hasText(src.getHost())) target.setHost(src.getHost());
        if (hasText(src.getPort())) target.setPort(src.getPort());
        if (hasText(src.getDbName())) target.setDbName(src.getDbName());
        if (hasText(src.getUsername())) target.setUsername(src.getUsername());
        if (src.getStatus() != null) target.setStatus(src.getStatus());

        if (hasText(src.getPassword())) {
            target.setPassword(encryptionUtil.encrypt(src.getPassword()));
        }
    }
}
