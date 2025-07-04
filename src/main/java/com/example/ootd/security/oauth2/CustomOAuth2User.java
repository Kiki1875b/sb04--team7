package com.example.ootd.security.oauth2;

import com.example.ootd.domain.user.User;
import com.example.ootd.security.PrincipalUser;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User, PrincipalUser {


  private final User user;
  private final Map<String, Object> attributes;

  @Override
  public User getUser (){
    return user;
  }


  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority(user.getRole().name()));
  }

  @Override
  public boolean isOAuthUser() {
    return true;
  }

  @Override
  public String getName() {
    return user.getId().toString();
  }
}
