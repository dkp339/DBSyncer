package com.rubberhuman.dbsyncer.controller.datasource;

import com.rubberhuman.dbsyncer.dto.datasource.StatusParam;
import com.rubberhuman.dbsyncer.entity.datasource.DataSourceConfig;
import com.rubberhuman.dbsyncer.service.datasource.DataSourceConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/datasource")
public class DataSourceConfigController {

    @Autowired
    private DataSourceConfigService dataSourceConfigService;

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody DataSourceConfig config) {
        dataSourceConfigService.addDataSource(config);
        return ResponseEntity.ok("添加成功");
    }

    @GetMapping("/list")
    public ResponseEntity<List<DataSourceConfig>> list() {
        List<DataSourceConfig> list = dataSourceConfigService.list();

        // 将返回的密码字段设置为 *，防止泄密
        list.forEach(item -> item.setPassword("******"));
        return ResponseEntity.ok(list);
    }

    @PostMapping("/test")
    public ResponseEntity<?> testConnection(@RequestBody DataSourceConfig config) {
        dataSourceConfigService.testConnection(config);
        return ResponseEntity.ok("连接成功");
    }


    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestBody DataSourceConfig config) {
        dataSourceConfigService.updateDataSource(config);
        return ResponseEntity.ok("配置更新成功");
    }


    @PutMapping("/status")
    public ResponseEntity<?> updateStatus(@RequestBody StatusParam param) {
        dataSourceConfigService.updateStatus(param.getId(), param.getStatus());
        return ResponseEntity.ok("状态更新成功");
    }

    // @TableLogic 会自动把 DELETE 语句转为 UPDATE deleted=1
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        dataSourceConfigService.removeById(id);
        return ResponseEntity.ok("删除成功");
    }
}
