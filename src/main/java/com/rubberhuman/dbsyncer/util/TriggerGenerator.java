package com.rubberhuman.dbsyncer.util;

import com.rubberhuman.dbsyncer.enums.datasource.DatabaseType;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.Index;


public class TriggerGenerator {

    // 同步账号名称，防止回环
    private static final String SYNC_USER = "dbsyncer";

    // 版本号字段名称
    private static final String VERSION_COL = "sync_version";

    public static String generate(String ddlSql, DatabaseType dbType) {
        StringBuilder result = new StringBuilder();

        try {
            if (dbType == DatabaseType.POSTGRESQL) {
                result.append(getPostgresFunction()).append("\n\n");
            }

            // 解析 SQL 语句
            String[] potentialStmts = ddlSql.split(";");

            for (String sqlStmt : potentialStmts) {
                if (sqlStmt.trim().isEmpty()) continue;
                try {
                    Statement statement = CCJSqlParserUtil.parse(sqlStmt);
                    if (statement instanceof CreateTable) {
                        CreateTable createTable = (CreateTable) statement;
                        String tableName = createTable.getTable().getName().replace("`", "").replace("\"", "");
                        String pkColumn = findPrimaryKey(createTable);

                        if (pkColumn != null) {
                            result.append(buildTriggerSql(tableName, pkColumn, dbType));
                            result.append("\n\n");
                        } else {
                            result.append("-- [WARN] 表 ").append(tableName).append(" 未找到主键，无法生成触发器\n");
                        }
                    }
                } catch (Exception e) {
                    // 忽略非 Create Table 语句或解析错误的语句
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "解析错误: " + e.getMessage();
        }

        return result.toString();
    }

    private static String findPrimaryKey(CreateTable createTable) {
        if (createTable.getColumnDefinitions() != null) {
            for (ColumnDefinition col : createTable.getColumnDefinitions()) {
                if (col.getColumnSpecs() != null) {
                    for (String spec : col.getColumnSpecs()) {
                        if ("PRIMARY".equalsIgnoreCase(spec) || "KEY".equalsIgnoreCase(spec)) {
                            if (col.getColumnSpecs().toString().toUpperCase().contains("PRIMARY")) {
                                return col.getColumnName();
                            }
                        }
                    }
                }
            }
        }

        if (createTable.getIndexes() != null) {
            for (Index index : createTable.getIndexes()) {
                if ("PRIMARY KEY".equalsIgnoreCase(index.getType())) {
                    // 返回第一个主键列
                    return index.getColumnsNames().get(0);
                }
            }
        }
        return null;
    }


    private static String buildTriggerSql(String tableName, String pkCol, DatabaseType dbType) {
        switch (dbType) {
            case MYSQL:
                return buildMysql(tableName, pkCol);
            case ORACLE:
                return buildOracle(tableName, pkCol);
            case POSTGRESQL:
                return buildPostgres(tableName, pkCol);
            case SQL_SERVER:
                return buildSqlServer(tableName, pkCol);
            default:
                return "-- 不支持的数据库类型: " + dbType.getDescription();
        }
    }

    // === MySQL 模板===
    private static String buildMysql(String table, String pk) {
        String tpl = "DELIMITER $$\n" +
                "-- [MySQL] %1$s 乐观锁触发器 \n" +
                "DROP TRIGGER IF EXISTS trg_%1$s_before_update $$ \n" +
                "CREATE TRIGGER trg_%1$s_before_update BEFORE UPDATE ON %1$s FOR EACH ROW BEGIN \n" +
                "    IF SUBSTRING_INDEX(USER(),'@',1) != '" + SYNC_USER + "' THEN \n" +
                "        SET NEW." + VERSION_COL + " = OLD." + VERSION_COL + " + 1; \n" +
                "    END IF; \n" +
                "END $$ \n\n" +

                "-- [MySQL] %1$s 同步日志触发器 \n" +
                "DROP TRIGGER IF EXISTS trg_%1$s_insert $$ \n" +
                "CREATE TRIGGER trg_%1$s_insert AFTER INSERT ON %1$s FOR EACH ROW BEGIN \n" +
                "    IF SUBSTRING_INDEX(USER(),'@',1) != '" + SYNC_USER + "' THEN \n" +
                "        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version) \n" +
                "        VALUES ('%1$s', 'INSERT', '%2$s', CAST(NEW.%2$s AS CHAR), 0, NOW(), 'MYSQL', NEW." + VERSION_COL + "); \n" +
                "    END IF; \n" +
                "END $$ \n\n" +

                "DROP TRIGGER IF EXISTS trg_%1$s_update $$ \n" +
                "CREATE TRIGGER trg_%1$s_update AFTER UPDATE ON %1$s FOR EACH ROW BEGIN \n" +
                "    IF SUBSTRING_INDEX(USER(),'@',1) != '" + SYNC_USER + "' THEN \n" +
                "        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version) \n" +
                "        VALUES ('%1$s', 'UPDATE', '%2$s', CAST(NEW.%2$s AS CHAR), 0, NOW(), 'MYSQL', NEW." + VERSION_COL + "); \n" +
                "    END IF; \n" +
                "END $$ \n\n" +

                "DROP TRIGGER IF EXISTS trg_%1$s_delete $$ \n" +
                "CREATE TRIGGER trg_%1$s_delete AFTER DELETE ON %1$s FOR EACH ROW BEGIN \n" +
                "    IF SUBSTRING_INDEX(USER(),'@',1) != '" + SYNC_USER + "' THEN \n" +
                "        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version) \n" +
                "        VALUES ('%1$s', 'DELETE', '%2$s', CAST(OLD.%2$s AS CHAR), 0, NOW(), 'MYSQL', OLD." + VERSION_COL + "); \n" +
                "    END IF; \n" +
                "END $$ \n" +
                "DELIMITER ;";
        return String.format(tpl, table, pk);
    }

    // === Oracle 模板 ===
    private static String buildOracle(String table, String pk) {
        // Oracle 不能在同一个触发器里既修改 :NEW 又做日志（容易变异表报错），建议分开
        String tpl = "-- [Oracle] %1$s 版本维护 (BEFORE UPDATE) \n" +
                "CREATE OR REPLACE TRIGGER trg_%1$s_ver \n" +
                "BEFORE UPDATE ON %1$s FOR EACH ROW \n" +
                "DECLARE \n" +
                "    v_user VARCHAR2(50); \n" +
                "BEGIN \n" +
                "    SELECT SYS_CONTEXT('USERENV', 'SESSION_USER') INTO v_user FROM DUAL; \n" +
                "    IF v_user != UPPER('" + SYNC_USER + "') THEN \n" +
                "        :NEW." + VERSION_COL + " := :OLD." + VERSION_COL + " + 1; \n" +
                "    END IF; \n" +
                "END; \n" +
                "/ \n\n" +

                "-- [Oracle] %1$s 同步日志 (AFTER I/U/D) \n" +
                "CREATE OR REPLACE TRIGGER trg_%1$s_sync \n" +
                "AFTER INSERT OR UPDATE OR DELETE ON %1$s FOR EACH ROW \n" +
                "DECLARE \n" +
                "    v_op VARCHAR2(10); \n" +
                "    v_pk VARCHAR2(255); \n" +
                "    v_ver NUMBER; \n" +
                "    v_user VARCHAR2(50); \n" +
                "BEGIN \n" +
                "    SELECT SYS_CONTEXT('USERENV', 'SESSION_USER') INTO v_user FROM DUAL; \n" +
                "    IF v_user != UPPER('" + SYNC_USER + "') THEN \n" +
                "        IF INSERTING THEN \n" +
                "            v_op := 'INSERT'; v_pk := TO_CHAR(:NEW.%2$s); v_ver := :NEW." + VERSION_COL + "; \n" +
                "        ELSIF UPDATING THEN \n" +
                "            v_op := 'UPDATE'; v_pk := TO_CHAR(:NEW.%2$s); v_ver := :NEW." + VERSION_COL + "; \n" +
                "        ELSIF DELETING THEN \n" +
                "            v_op := 'DELETE'; v_pk := TO_CHAR(:OLD.%2$s); v_ver := :OLD." + VERSION_COL + "; \n" +
                "        END IF; \n" +
                "        INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version) \n" +
                "        VALUES ('%1$s', v_op, '%2$s', v_pk, 0, SYSDATE, 'ORACLE', v_ver); \n" +
                "    END IF; \n" +
                "END; \n" +
                "/";
        return String.format(tpl, table, pk);
    }

    // === PostgreSQL 模板===
    private static String getPostgresFunction() {
        return "-- [PG] 1. 通用版本自增函数 \n" +
                "CREATE OR REPLACE FUNCTION increment_version() RETURNS TRIGGER AS $$\n" +
                "BEGIN\n" +
                "    IF CURRENT_USER != '" + SYNC_USER + "' THEN\n" +
                "        NEW." + VERSION_COL + " := OLD." + VERSION_COL + " + 1;\n" +
                "    END IF;\n" +
                "    RETURN NEW;\n" +
                "END;\n" +
                "$$ LANGUAGE plpgsql;\n\n" +

                "-- [PG] 2. 通用日志记录函数 \n" +
                "CREATE OR REPLACE FUNCTION notify_sync_event() RETURNS TRIGGER AS $$\n" +
                "DECLARE\n" +
                "    current_pk_value VARCHAR;\n" +
                "    current_version BIGINT;\n" +
                "    pk_col_name VARCHAR;\n" +
                "BEGIN\n" +
                "    IF CURRENT_USER = '" + SYNC_USER + "' THEN RETURN NULL; END IF;\n" +
                "    pk_col_name := TG_ARGV[0];\n" +
                "    \n" +
                "    IF (TG_OP = 'DELETE') THEN\n" +
                "        EXECUTE 'SELECT $1.' || pk_col_name USING OLD INTO current_pk_value;\n" +
                "        -- DELETE 时记录旧版本号 \n" +
                "        EXECUTE 'SELECT $1." + VERSION_COL + "' USING OLD INTO current_version;\n" +
                "    ELSE\n" +
                "        EXECUTE 'SELECT $1.' || pk_col_name USING NEW INTO current_pk_value;\n" +
                "        -- INSERT/UPDATE 时记录新版本号 \n" +
                "        EXECUTE 'SELECT $1." + VERSION_COL + "' USING NEW INTO current_version;\n" +
                "    END IF;\n" +
                "    \n" +
                "    INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version) \n" +
                "    VALUES (TG_TABLE_NAME, TG_OP, pk_col_name, current_pk_value, 0, NOW(), 'POSTGRESQL', current_version);\n" +
                "    RETURN NULL;\n" +
                "END;\n" +
                "$$ LANGUAGE plpgsql;";
    }

    private static String buildPostgres(String table, String pk) {
        String tpl = "-- %1$s 触发器绑定 (PostgreSQL)\n" +
                // 1. 绑定版本自增 (BEFORE UPDATE)
                "DROP TRIGGER IF EXISTS trg_%1$s_ver ON %1$s;\n" +
                "CREATE TRIGGER trg_%1$s_ver BEFORE UPDATE ON %1$s \n" +
                "FOR EACH ROW EXECUTE FUNCTION increment_version();\n\n" +
                // 2. 绑定日志记录 (AFTER I/U/D)
                "DROP TRIGGER IF EXISTS trg_%1$s_sync ON %1$s;\n" +
                "CREATE TRIGGER trg_%1$s_sync AFTER INSERT OR UPDATE OR DELETE ON %1$s \n" +
                "FOR EACH ROW EXECUTE FUNCTION notify_sync_event('%2$s');";
        return String.format(tpl, table, pk);
    }

    // === SQL Server 模板  ===
    private static String buildSqlServer(String table, String pk) {
        // SQL Server 没有 BEFORE UPDATE。
        // 做法：在 AFTER UPDATE 里，手动再次更新表，把 version + 1
        // 注意：需要防止递归触发 (UPDATE -> Trigger -> UPDATE -> Trigger...)
        // 通常 SQL Server 默认递归触发器是禁用的，或者我们通过 logic 判断

        String tpl = "-- [SQL Server] %1$s 触发器 \n" +
                "CREATE OR ALTER TRIGGER trg_%1$s_sync ON %1$s \n" +
                "AFTER INSERT, UPDATE, DELETE \n" +
                "AS \n" +
                "BEGIN \n" +
                "    SET NOCOUNT ON; \n" +
                "    IF SUSER_NAME() <> '" + SYNC_USER + "' \n" +
                "    BEGIN \n" +
                "        -- 1. 处理 UPDATE: 先自增版本号，再记录日志 \n" +
                "        IF EXISTS(SELECT * FROM inserted) AND EXISTS(SELECT * FROM deleted) \n" +
                "        BEGIN \n" +
                "            -- 核心：手动更新版本号 (模拟 BEFORE 逻辑) \n" +
                "            -- 只更新那些 sync_version 没有被显式修改的行，或者强制+1 \n" +
                "            UPDATE t \n" +
                "            SET t." + VERSION_COL + " = ISNULL(d." + VERSION_COL + ",0) + 1 \n" +
                "            FROM %1$s t \n" +
                "            INNER JOIN deleted d ON t.%2$s = d.%2$s \n" +
                "            INNER JOIN inserted i ON t.%2$s = i.%2$s; \n" +
                "            \n" +
                "            -- 记录日志 (注意：此时表中数据已经是 +1 后的了) \n" +
                "            INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version) \n" +
                "            SELECT '%1$s', 'UPDATE', '%2$s', CAST(i.%2$s AS VARCHAR(255)), 0, GETDATE(), 'SQL_SERVER', ISNULL(d." + VERSION_COL + ",0) + 1 \n" +
                "            FROM inserted i JOIN deleted d ON i.%2$s = d.%2$s; \n" +
                "        END \n" +
                "        \n" +
                "        -- 2. 处理 INSERT \n" +
                "        ELSE IF EXISTS(SELECT * FROM inserted) \n" +
                "        BEGIN \n" +
                "            INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version) \n" +
                "            SELECT '%1$s', 'INSERT', '%2$s', CAST(i.%2$s AS VARCHAR(255)), 0, GETDATE(), 'SQL_SERVER', i." + VERSION_COL + " \n" +
                "            FROM inserted i; \n" +
                "        END \n" +
                "        \n" +
                "        -- 3. 处理 DELETE \n" +
                "        ELSE IF EXISTS(SELECT * FROM deleted) \n" +
                "        BEGIN \n" +
                "            INSERT INTO sync_event (table_name, op_type, pk_column_name, pk_value, status, op_time, source_db_type, data_version) \n" +
                "            SELECT '%1$s', 'DELETE', '%2$s', CAST(d.%2$s AS VARCHAR(255)), 0, GETDATE(), 'SQL_SERVER', d." + VERSION_COL + " \n" +
                "            FROM deleted d; \n" +
                "        END \n" +
                "    END \n" +
                "END;";
        return String.format(tpl, table, pk);
    }
}