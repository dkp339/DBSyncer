package com.rubberhuman.dbsyncer.service.sync;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Slf4j
public class SyncAlertServiceImpl implements SyncAlertService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${dbsyncer.admin-email}")
    private String adminEmail;

    @Async
    @Override
    public void sendConflictAlert(String sourceDbName, String targetDbName,
                                  String tableName, String pkVal, String errorMsg) {
        try {
            log.info("正在发送冲突报警邮件...");

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(adminEmail);
            message.setSubject("数据库同步冲突报警 - " + tableName);

            StringBuilder text = new StringBuilder();
            text.append("管理员，您好：\n\n");
            text.append("同步系统检测到数据版本冲突，详情如下：\n");
            text.append("--------------------------------------------------\n");
            text.append("冲突时间：").append(new Date()).append("\n");
            text.append("源数据库：").append(sourceDbName).append("\n");
            text.append("目标数据库：").append(targetDbName).append("\n");
            text.append("冲突表名：").append(tableName).append("\n");
            text.append("主键值：").append(pkVal).append("\n");
            text.append("--------------------------------------------------\n");
            text.append("错误详情：\n").append(errorMsg).append("\n\n");
            text.append("请登录管理后台人工修复数据：/api/admin/sync/logs");

            message.setText(text.toString());

            mailSender.send(message);
            log.info("冲突报警邮件发送成功！");

        } catch (Exception e) {
            // 发邮件失败不能影响主程序运行，记个日志就行
            log.error("邮件发送失败: {}", e.getMessage());
        }
    }
}
