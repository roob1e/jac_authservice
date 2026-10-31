package com.innowise.authservice.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ApiErrorAccessDeniedHandler implements AccessDeniedHandler {
  private final ApiErrorResponseWriter writer;

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e)
          throws IOException {
    writer.write(request, response, HttpStatus.FORBIDDEN, "Access denied");
  }
}
