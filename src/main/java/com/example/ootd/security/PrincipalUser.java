package com.example.ootd.security;

import com.example.ootd.domain.user.User;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

public interface PrincipalUser {
  User getUser();
  Collection<? extends GrantedAuthority> getAuthorities();
  boolean isOAuthUser();

}
