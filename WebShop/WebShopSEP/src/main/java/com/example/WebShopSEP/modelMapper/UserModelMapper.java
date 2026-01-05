package com.example.WebShopSEP.modelMapper;

import com.example.WebShopSEP.dto.user.UserDto;
import com.example.WebShopSEP.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {})
public interface UserModelMapper {
    UserDto toDto(User user);
    User toEntity(UserDto userDto);
}
