package com.innowise.authservice.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ApiErrorAuthenticationEntryPoint implements AuthenticationEntryPoint {
  private final ApiErrorResponseWriter writer;

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
          throws IOException {
    writer.write(request, response, HttpStatus.UNAUTHORIZED, "Authentication required");
  }
}
