package com.mcm.privatecircle.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import com.mcm.privatecircle.global.security.AuthenticatedUser;
import com.mcm.privatecircle.global.security.JwtTokenProvider;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class JwtSecurityHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void 만료_JWT는_실제_HTTP에서_TOKEN_EXPIRED를_반환한다() throws Exception {
        JwtTokenProvider expiredProvider = new JwtTokenProvider(
            jwtTokenProvider.getSecret(), Duration.ofSeconds(-1)
        );
        String token = expiredProvider.createAccessToken(AuthenticatedUser.customer(1L, 2L));

        mockMvc.perform(get("/api/v1/customers/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("TOKEN_EXPIRED"));
    }

    @Test
    void 위조되거나_형식이_잘못된_JWT는_실제_HTTP에서_INVALID_TOKEN을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/customers/me")
                .header("Authorization", "Bearer forged-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    @Test
    void 유효한_서명이지만_Role이_잘못된_JWT도_INVALID_TOKEN이다() throws Exception {
        String token = signedToken("ADMIN", true);

        mockMvc.perform(get("/api/v1/customers/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    @Test
    void 필수_Customer_Claim이_누락된_JWT도_INVALID_TOKEN이다() throws Exception {
        String token = signedToken("CUSTOMER", false);

        mockMvc.perform(get("/api/v1/customers/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    private String signedToken(String role, boolean includeCustomerId) throws Exception {
        Instant now = Instant.now();
        var builder = Jwts.builder()
            .subject("1")
            .claim("accountId", 1L)
            .claim("role", role)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(300)));
        if (includeCustomerId) {
            builder.claim("customerId", 2L);
        }
        return builder.signWith(secretKey()).compact();
    }

    private SecretKey secretKey() throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
            .digest(jwtTokenProvider.getSecret().getBytes(StandardCharsets.UTF_8));
        return Keys.hmacShaKeyFor(digest);
    }
}