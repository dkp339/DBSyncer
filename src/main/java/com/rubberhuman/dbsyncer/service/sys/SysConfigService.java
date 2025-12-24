package com.rubberhuman.dbsyncer.service.sys;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rubberhuman.dbsyncer.entity.sys.SysConfig;

public interface SysConfigService extends IService<SysConfig> {
    String getSyncCron();
    void updateSyncCron(String cronExpression);
}