package by.assxmblxr.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record RegisterRequest(
        @NotBlank
        @Size(min = 5, max = 100, message = "Login must be 5-100 characters long")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Login may contain only Latin letters, digits and \"_\"")
        String login,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 64, message = "Password must be 8-64 characters long")
        @Pattern(
                regexp = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])[!-~]+$",
                message = "Password must contain uppercase and lowercase Latin letters and digits"
        )
        String password,

        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName
) {
}
