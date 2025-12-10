package com.rubberhuman.dbsyncer.service.auth;

import com.rubberhuman.dbsyncer.entity.auth.AppUser;
import com.rubberhuman.dbsyncer.mapper.auth.AppUserMapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class AppUserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private AppUserMapper appUserMapper;

    @Override
    public UserDetails loadUserByUsername(String username) {

        // 1. 查询 app_users 表中的数据
        QueryWrapper<AppUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        AppUser appUser = appUserMapper.selectOne(queryWrapper);

        // 2. 若用户不存在，抛出异常
        if (appUser == null) {
            throw new UsernameNotFoundException("用户 " + username + " 不存在");
        }

        // 3. 创建权限列表，Spring Security 需要 'ROLE_' 前缀
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + appUser.getRole())     // 例如 "ROLE_ADMIN"
        );

        // 4. 返回 Spring Security 的 User 对象
        // 它会使用 appUser 的密码(哈希)和登录时提交的密码(明文)进行比对
        return new User(appUser.getUsername(), appUser.getPassword(), authorities);
    }
}
