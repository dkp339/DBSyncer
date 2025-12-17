package com.rubberhuman.dbsyncer;

import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Encoders;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.crypto.SecretKey;
import java.security.SecureRandom;


class MyTest {
    @Test
    public void test() {
        SecretKey key = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS512);
        String base64Key = Encoders.BASE64.encode(key.getEncoded());
        System.out.println("JWT_BASE64_KEY=" + base64Key);
    }
}

