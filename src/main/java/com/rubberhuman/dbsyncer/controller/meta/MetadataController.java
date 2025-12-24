package com.rubberhuman.dbsyncer.controller.meta;

import com.rubberhuman.dbsyncer.dto.meta.SqlQueryRequest;
import com.rubberhuman.dbsyncer.service.meta.MetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meta")
public class MetadataController {

    @Autowired
    private MetadataService metadataService;

    @GetMapping("/tables")
    public ResponseEntity<List<String>> listTables(@RequestParam Long sourceId) {
        List<String> tables = metadataService.listTables(sourceId);
        return ResponseEntity.ok(tables);
    }

    @PostMapping("/query")
    public ResponseEntity<List<Map<String, Object>>> executeQuery(@RequestBody @Validated SqlQueryRequest request) {
        List<Map<String, Object>> result = metadataService.executeSql(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/explain")
    public ResponseEntity<List<Map<String, Object>>> explainSql(@RequestBody @Validated SqlQueryRequest request) {
        List<Map<String, Object>> result = metadataService.explainSql(request);
        return ResponseEntity.ok(result);
    }
}
