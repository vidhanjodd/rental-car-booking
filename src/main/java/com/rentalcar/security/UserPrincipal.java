package com.rentalcar.security;

import com.rentalcar.entity.User;
import com.rentalcar.enums.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String username;
    private final String email;
    private final String password;
    private final Role role;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    private UserPrincipal(User user) {
        this.id          = user.getId();
        this.username    = user.getUsername();
        this.email       = user.getEmail();
        this.password    = user.getPassword();
        this.role        = user.getRole();
        this.enabled     = user.isEnabled();
        this.authorities = List.of(new SimpleGrantedAuthority(user.getRole().name()));
    }

    public static UserPrincipal from(User user) {
        return new UserPrincipal(user);
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword()                                      { return password; }
    @Override public String getUsername()                                      { return username; }
    @Override public boolean isAccountNonExpired()                             { return true; }
    @Override public boolean isAccountNonLocked()                              { return true; }
    @Override public boolean isCredentialsNonExpired()                         { return true; }
    @Override public boolean isEnabled()                                       { return enabled; }
}
