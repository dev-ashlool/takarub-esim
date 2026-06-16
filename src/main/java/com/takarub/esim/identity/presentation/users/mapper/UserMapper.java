package com.takarub.esim.identity.presentation.users.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.takarub.esim.identity.application.query.GetUserByEmailQuery;
import com.takarub.esim.identity.application.query.GetUserByIdQuery;
import com.takarub.esim.identity.application.result.UserView;
import com.takarub.esim.identity.presentation.users.response.UserResponse;

@Component
public class UserMapper {

    public GetUserByIdQuery toQuery(String userId) {
        return new GetUserByIdQuery(userId);
    }

    public GetUserByEmailQuery toQueryByEmail(String email) {
        return new GetUserByEmailQuery(email);
    }

    public UserResponse toResponse(UserView view) {
        List<String> roles = view.roles().stream().map(Enum::name).sorted().toList();
        return new UserResponse(
                view.id().value().toString(),
                view.email().value(),
                view.status().name(),
                roles,
                view.createdAt(),
                view.updatedAt());
    }
}
