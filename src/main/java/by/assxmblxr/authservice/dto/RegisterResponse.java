package by.assxmblxr.authservice.dto;

import by.assxmblxr.authservice.entity.Role;

public record RegisterResponse(
        Long id,
        Long userId,
        String login,
        Role role
) {
}
