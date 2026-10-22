package by.assxmblxr.authservice.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
