package com.rubberhuman.dbsyncer.service.sync;

public interface SyncAlertService {
    void sendConflictAlert(String sourceDbName, String targetDbName, String tableName, String pkVal, String errorMsg);
}
