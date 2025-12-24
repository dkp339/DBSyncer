package com.rubberhuman.dbsyncer.controller.sys;

import com.rubberhuman.dbsyncer.service.sys.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/config")
public class SysConfigController {

    @Autowired
    private SysConfigService sysConfigService;

    @GetMapping("/cron")
    public ResponseEntity<String> getCron() {
        return ResponseEntity.ok(sysConfigService.getSyncCron());
    }

    @PostMapping("/cron")
    public ResponseEntity<String> updateCron(@RequestBody Map<String, String> body) {
        String cron = body.get("cron");
        if (cron == null || cron.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("参数 cron 不能为空");
        }

        sysConfigService.updateSyncCron(cron);
        return ResponseEntity.ok("更新成功，下次任务调度将生效");
    }
}
