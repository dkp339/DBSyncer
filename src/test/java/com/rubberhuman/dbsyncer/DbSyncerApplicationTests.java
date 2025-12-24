package com.rubberhuman.dbsyncer;

import com.rubberhuman.dbsyncer.enums.datasource.DatabaseType;
import com.rubberhuman.dbsyncer.util.EncryptionUtil;
import com.rubberhuman.dbsyncer.util.TriggerGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class GenerateCodeTest {
    @Test
    public void test() throws IOException {

        // 把你上传的 mysql_init.sql 的内容粘贴在这里
        // 注意：Java 字符串里换行比较麻烦，JDK15+ 可以用文本块 """ """
        // 如果是 JDK8，你需要手动拼接或者从文件读取


//        String mysqlSql = Files.readString(
//                Path.of("sql/mysql/mysql_init.sql")
//        );
//
//        String postgresSql = Files.readString(
//                Path.of("sql/postgre/postgre_init.sql")
//        );
//
//        String oracleSql = Files.readString(
//                Path.of("sql/oracle/oracle_init.sql")
//        );
//
//
//        System.out.println("============== MySQL Trigger Script ==============");
//        System.out.println(TriggerGenerator.generate(mysqlSql, DatabaseType.MYSQL));
//
//        System.out.println("============== Postgres Trigger Script ==============");
//        System.out.println(TriggerGenerator.generate(postgresSql, DatabaseType.POSTGRESQL));
//
//        System.out.println("============== Oracle Trigger Script ==============");
//        System.out.println(TriggerGenerator.generate(oracleSql, DatabaseType.ORACLE));

    }
}