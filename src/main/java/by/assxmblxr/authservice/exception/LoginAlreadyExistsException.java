package by.assxmblxr.authservice.exception;

public class LoginAlreadyExistsException extends RuntimeException {
  public LoginAlreadyExistsException(String message) {
    super(message);
  }
}
