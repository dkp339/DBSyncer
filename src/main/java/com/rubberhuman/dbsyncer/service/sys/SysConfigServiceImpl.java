package com.rubberhuman.dbsyncer.service.sys;

import com.rubberhuman.dbsyncer.exception.BusinessException;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.rubberhuman.dbsyncer.entity.sys.SysConfig;
import com.rubberhuman.dbsyncer.mapper.sys.SysConfigMapper;

@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    private static final String KEY_SYNC_CRON = "sync.cron";
    private static final String DEFAULT_CRON = "0 0 1 * * ?"; // 默认每天凌晨1点

    @Override
    public String getSyncCron() {
        SysConfig config = this.getById(KEY_SYNC_CRON);
        if (config == null || config.getConfigValue() == null) {
            return DEFAULT_CRON;
        }
        return config.getConfigValue();
    }

    @Override
    public void updateSyncCron(String cron) {
        // 1. 校验 Cron 表达式合法性 (Spring 自带工具)
        if (!CronExpression.isValidExpression(cron)) {
            throw new BusinessException("Cron 表达式格式无效");
        }

        // 2. 保存或更新
        SysConfig config = new SysConfig();
        config.setConfigKey(KEY_SYNC_CRON);
        config.setConfigValue(cron);
        config.setDescription("周期同步Cron表达式");

        this.saveOrUpdate(config);
    }
}
