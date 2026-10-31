package com.innowise.authservice.security;

import com.innowise.authservice.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ApiErrorResponseWriter {
  private final JsonMapper jsonMapper;

  public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message)
          throws IOException {
    ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message,
            request.getRequestURI());
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    jsonMapper.writeValue(response.getOutputStream(), body);
  }
}
