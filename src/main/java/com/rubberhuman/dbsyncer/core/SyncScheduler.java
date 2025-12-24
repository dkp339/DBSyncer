package com.rubberhuman.dbsyncer.core;

import com.rubberhuman.dbsyncer.entity.datasource.DataSourceConfig;
import com.rubberhuman.dbsyncer.service.datasource.DataSourceConfigService;
import com.rubberhuman.dbsyncer.service.sync.SyncEventService;
import com.rubberhuman.dbsyncer.service.sys.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import java.util.List;

@Configuration
@EnableScheduling
@Slf4j
public class SyncScheduler implements SchedulingConfigurer {

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private DataSourceConfigService dataSourceConfigService;

    @Autowired
    private SyncEventService syncEventService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(
                this::executePeriodicSync,
                triggerContext -> {
                    String cron = sysConfigService.getSyncCron();
                    try {
                        return new CronTrigger(cron).nextExecution(triggerContext);
                    } catch (Exception e) {
                        log.error("Cron 表达式非法，使用默认值: {}", cron, e);
                        return new CronTrigger("0 0 1 * * ?").nextExecution(triggerContext);
                    }
                }
        );

    }

    // 将 status=2 (失败) 的任务重置为 status=0 (未处理)，
    // 这样 SyncEngine 的实时轮询线程就会自动捡起它们重新尝试。
    private void executePeriodicSync() {
        log.info(">>> [周期调度器] 开始执行定时同步任务...");

        try {
            List<DataSourceConfig> activeSources = dataSourceConfigService.listEnabled();

            if (activeSources == null || activeSources.isEmpty()) {
                log.info(">>> [周期调度器] 无启用数据源，跳过");
                return;
            }

            int totalReset = 0;
            for (DataSourceConfig source : activeSources) {
                int count = syncEventService.resetFailedEventsInPast24Hours(source.getSourceId());
                if (count > 0) {
                    log.info(">>> [周期调度器] 数据源[{}]：已重置 {} 条失败事件，等待引擎重试", source.getSourceName(), count);
                    totalReset += count;
                }
            }

            log.info(">>> [周期调度器] 任务结束，共重置 {} 条事件", totalReset);

        } catch (Exception e) {
            log.error(">>> [周期调度器] 执行异常", e);
        }
    }
}