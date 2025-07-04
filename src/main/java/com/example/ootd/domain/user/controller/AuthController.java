package com.example.ootd.domain.user.controller;


import com.example.ootd.domain.user.dto.ResetPasswordRequest;
import com.example.ootd.domain.user.dto.UserCreateRequest;
import com.example.ootd.domain.user.dto.UserDto;
import com.example.ootd.domain.user.service.AuthService;
import com.example.ootd.domain.user.service.UserService;
import com.example.ootd.exception.ErrorCode;
import com.example.ootd.exception.OotdException;
import com.example.ootd.security.jwt.JwtService;
import com.example.ootd.security.jwt.JwtSession;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {


  private final AuthService authService;

  @PostMapping("/users")
  public ResponseEntity<UserDto> registerUser(@RequestBody UserCreateRequest request){
    UserDto dto = authService.registerUser(request);
    return ResponseEntity.ok(dto);
  }

  @PostMapping("/auth/refresh")
  public ResponseEntity<String> refreshToken(HttpServletRequest req, HttpServletResponse res) {
    return ResponseEntity.ok(authService.refreshToken(req,res));
  }

  @PostMapping("/auth/sign-out")
  public ResponseEntity<Void> signOut(HttpServletRequest request, HttpServletResponse response){
    authService.signOut(request, response);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/auth/me")
  public ResponseEntity<String> getAccessTokenFromRefreshToken(HttpServletRequest request){
    return ResponseEntity.ok(authService.getAccessToken(request));
  }

  @PostMapping("/auth/reset-password")
  public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request){
    authService.resetPassword(request);
    return ResponseEntity.ok().build();
  }
}
