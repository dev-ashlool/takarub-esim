package com.takarub.esim.identity.application.usecase;

import com.takarub.esim.identity.application.exception.UserNotFoundApplicationException;
import com.takarub.esim.identity.application.query.GetUserByIdQuery;
import com.takarub.esim.identity.application.result.UserView;
import com.takarub.esim.identity.domain.user.User;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.domain.user.UserRepository;

/**
 * Read-only query use case: loads a user by identifier and projects it to a {@link UserView}.
 */
public class GetUserByIdUseCase {

    private final UserRepository userRepository;

    public GetUserByIdUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserView execute(GetUserByIdQuery query) {
        UserId userId = UserId.of(query.userId());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundApplicationException(userId));
        return UserView.from(user);
    }
}
