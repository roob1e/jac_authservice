package by.assxmblxr.authservice.client;

public interface UserServiceClient {
  Long createUser(String firstName, String lastName, String email);

  void deleteUser(Long id);
}
