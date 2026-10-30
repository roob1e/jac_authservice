package by.assxmblxr.authservice.client;

import java.time.LocalDate;

public interface UserServiceClient {
  Long createUser(String name, String surname, String email, LocalDate birthDate);

  void deleteUser(Long id);
}
