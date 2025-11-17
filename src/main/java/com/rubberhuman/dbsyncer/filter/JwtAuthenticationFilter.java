package com.rubberhuman.dbsyncer.filter;

import com.rubberhuman.dbsyncer.service.auth.AppUserDetailsService;
import com.rubberhuman.dbsyncer.util.JwtTokenUtil;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private AppUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
        throws ServletException, IOException {

        // 1. 从请求头获取 Token
        final String requestTokenHeader = req.getHeader("Authorization");

        String username = null;
        String jwtToken = null;

        // 2. 检查 Token 是否存在，是否以 "Bearer " 开头
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                username = jwtTokenUtil.getUsernameFromToken(jwtToken);
            } catch (IllegalArgumentException e) {
                logger.warn("无法获取 JWT");
            } catch (ExpiredJwtException e) {
                logger.warn("JWT 已过期");
            }
        } else {
            logger.warn("请求头缺少 Bearer Token");
        }

        // 3. 验证 Token
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Token 有效，且当前 SecurityContext 中没有用户

            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // 4. 验证 Token 是否合法
            if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {

                // 5. 关键：手动设置认证信息
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));

                // 6. 将认证信息存入 SecurityContext，表明该用户已登录
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }

        // 7. 继续执行过滤器链
        chain.doFilter(req, resp);
    }


}
