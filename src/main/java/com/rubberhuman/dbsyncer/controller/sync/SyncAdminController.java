package com.rubberhuman.dbsyncer.controller.sync;

import com.rubberhuman.dbsyncer.dto.sync.SyncStats;
import com.rubberhuman.dbsyncer.entity.sync.SyncEvent;
import com.rubberhuman.dbsyncer.service.sync.SyncAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/sync")
public class SyncAdminController {

    @Autowired
    private SyncAdminService syncAdminService;

    // 获取同步统计信息
    @GetMapping("/stats")
    public ResponseEntity<SyncStats> getStats(@RequestParam Long sourceId) {
        SyncStats stats = syncAdminService.getStats(sourceId);
        return ResponseEntity.ok(stats);
    }

    // 分页查询日志
    @GetMapping("/logs")
    public ResponseEntity<List<SyncEvent>> listLogs(
            @RequestParam Long sourceId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<SyncEvent> logs = syncAdminService.listLogs(sourceId, status, page, size);
        return ResponseEntity.ok(logs);
    }

    // 重试同步事件
    @PostMapping("/retry/{eventId}")
    public ResponseEntity<?> retryEvent(
            @RequestParam Long sourceId,
            @PathVariable Long eventId) {

        syncAdminService.retryEvent(sourceId, eventId);
        return ResponseEntity.ok("操作成功，事件已重置，等待下一次轮询");
    }

    // 强制跳过事件 (POST 动作)
    @PostMapping("/skip/{eventId}")
    public ResponseEntity<?> skipEvent(
            @RequestParam Long sourceId,
            @PathVariable Long eventId) {

        syncAdminService.skipEvent(sourceId, eventId);
        return ResponseEntity.ok("操作成功，事件已标记为完成");
    }
}