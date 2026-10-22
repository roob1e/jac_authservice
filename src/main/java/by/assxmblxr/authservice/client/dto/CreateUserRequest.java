package by.assxmblxr.authservice.client.dto;

public record CreateUserRequest(
        String firstName,
        String lastName,
        String email
) {
}
