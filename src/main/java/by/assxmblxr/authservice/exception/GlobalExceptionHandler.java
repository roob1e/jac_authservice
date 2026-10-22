package by.assxmblxr.authservice.exception;

import by.assxmblxr.authservice.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(LoginAlreadyExistsException.class)
  public ResponseEntity<ApiError> handleLoginAlreadyExistsException(
          LoginAlreadyExistsException e, HttpServletRequest request
  ) {
    return buildResponse(HttpStatus.CONFLICT, e.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleMethodArgumentNotValidException(
          MethodArgumentNotValidException e, HttpServletRequest request
  ) {
    String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
    return buildResponse(HttpStatus.BAD_REQUEST, message, request);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleHttpMessageNotReadableException(
          HttpMessageNotReadableException e, HttpServletRequest request
  ) {
    return buildResponse(HttpStatus.BAD_REQUEST, "Malformed request body", request);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiError> handleDataIntegrityViolationException(
          DataIntegrityViolationException e, HttpServletRequest request
  ) {
    log.warn("Data integrity violation on {}", request.getRequestURI(), e);
    return buildResponse(HttpStatus.CONFLICT, "Data integrity violation", request);
  }

  @ExceptionHandler(RestClientResponseException.class)
  public ResponseEntity<ApiError> handleRestClientResponseException(
          RestClientResponseException e, HttpServletRequest request
  ) {
    HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
    if (status == null || !status.is4xxClientError()) {
      log.error("UserService returned {} on {}", e.getStatusCode(), request.getRequestURI(), e);
      return buildResponse(HttpStatus.BAD_GATEWAY, "User service failed to process the request", request);
    }
    return buildResponse(status, userServiceMessage(e), request);
  }

  @ExceptionHandler(RestClientException.class)
  public ResponseEntity<ApiError> handleRestClientException(RestClientException e, HttpServletRequest request) {
    log.error("User service is unavailable on {}", request.getRequestURI(), e);
    return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "User service is unavailable", request);
  }

  @ExceptionHandler(AccountDisabledException.class)
  public ResponseEntity<ApiError> handleAccountDisabledException(AccountDisabledException e, HttpServletRequest request) {
    return buildResponse(HttpStatus.FORBIDDEN, e.getMessage(), request);
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ApiError> handleInvalidCredentialsException(InvalidCredentialsException e, HttpServletRequest request) {
    return buildResponse(HttpStatus.UNAUTHORIZED, e.getMessage(), request);
  }

  @ExceptionHandler(InvalidTokenException.class)
  public ResponseEntity<ApiError> handleInvalidTokenException(InvalidTokenException e, HttpServletRequest request) {
    return buildResponse(HttpStatus.UNAUTHORIZED, e.getMessage(), request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleException(Exception e, HttpServletRequest request) {
    if (e instanceof ErrorResponse springError) {
      HttpStatus status = HttpStatus.resolve(springError.getStatusCode().value());
      if (status != null) {
        return buildResponse(status, e.getMessage(), request);
      }
    }
    log.error("Unexpected error on {}", request.getRequestURI(), e);
    return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request);
  }



  private String userServiceMessage(RestClientResponseException e) {
    try {
      ApiError body = e.getResponseBodyAs(ApiError.class);
      if (body != null && body.message() != null) {
        return body.message();
      }
    } catch (RuntimeException ignored) {}
    return "User service rejected the request";
  }

  private ResponseEntity<ApiError> buildResponse(HttpStatus status, String message, HttpServletRequest request) {
    ApiError body = new ApiError(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI()
    );
    return ResponseEntity.status(status).body(body);
  }
}
