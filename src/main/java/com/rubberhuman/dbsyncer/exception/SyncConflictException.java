package com.rubberhuman.dbsyncer.exception;

import lombok.Getter;

public class SyncConflictException extends BusinessException {

    @Getter
    private final String table;

    @Getter
    private final String pk;

    @Getter
    private final Long eventVersion;

    @Getter
    private final Long targetVersion;

    public SyncConflictException(String message, String table, String pk, Long eventVersion, Long targetVersion) {
        super(message);
        this.table = table;
        this.pk = pk;
        this.eventVersion = eventVersion;
        this.targetVersion = targetVersion;
    }
}

