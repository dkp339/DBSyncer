package com.rubberhuman.dbsyncer.entity.sys;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_config")
public class SysConfig {

    // config_key 是主键
    @TableId(value = "config_key")
    private String configKey;

    private String configValue;

    private String description;
}
