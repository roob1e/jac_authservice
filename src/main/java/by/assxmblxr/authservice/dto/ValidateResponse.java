package by.assxmblxr.authservice.dto;

public record ValidateResponse(
        Long userId,
        String username,
        String role
) {
}
