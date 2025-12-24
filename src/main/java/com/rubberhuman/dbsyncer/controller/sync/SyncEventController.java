package com.rubberhuman.dbsyncer.controller.sync;

import com.rubberhuman.dbsyncer.entity.sync.SyncEvent;
import com.rubberhuman.dbsyncer.service.sync.SyncEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sync-event")
public class SyncEventController {

    @Autowired
    private SyncEventService syncEventService;

    @GetMapping("/list")
    public ResponseEntity<List<SyncEvent>> list(@RequestParam Long sourceId) {
        List<SyncEvent> logs = syncEventService.getRecentLogs(sourceId);
        return ResponseEntity.ok(logs);
    }
}
