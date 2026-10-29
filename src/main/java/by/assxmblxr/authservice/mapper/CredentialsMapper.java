package by.assxmblxr.authservice.mapper;

import by.assxmblxr.authservice.dto.RegisterRequest;
import by.assxmblxr.authservice.dto.RegisterResponse;
import by.assxmblxr.authservice.entity.Credentials;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CredentialsMapper {
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "userId", ignore = true)
  @Mapping(target = "passwordHash", ignore = true)
  @Mapping(target = "role", ignore = true)
  @Mapping(target = "active", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Credentials toEntity(RegisterRequest request);

  RegisterResponse toRegisterResponse(Credentials credentials);
}
