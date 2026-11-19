package com.wjys.security;

import com.wjys.module.system.entity.SysUser;
import com.wjys.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WjysUserDetailsService implements UserDetailsService {
    private final SysUserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return toPrincipal(user);
    }

    public WjysPrincipal loadPrincipalById(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return toPrincipal(user);
    }

    private WjysPrincipal toPrincipal(SysUser user) {
        return new WjysPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getNickname(),
                user.getStatus(),
                user.getTokenVersion(),
                userMapper.selectRoleCodes(user.getId()),
                userMapper.selectPermissionCodes(user.getId())
        );
    }
}
