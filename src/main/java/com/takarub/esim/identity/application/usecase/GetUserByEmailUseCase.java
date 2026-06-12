package com.takarub.esim.identity.application.usecase;

import com.takarub.esim.identity.application.exception.UserNotFoundApplicationException;
import com.takarub.esim.identity.application.query.GetUserByEmailQuery;
import com.takarub.esim.identity.application.result.UserView;
import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.user.User;
import com.takarub.esim.identity.domain.user.UserRepository;

/**
 * Read-only query use case: loads a user by e-mail and projects it to a {@link UserView}.
 */
public class GetUserByEmailUseCase {

    private final UserRepository userRepository;

    public GetUserByEmailUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserView execute(GetUserByEmailQuery query) {
        EmailAddress email = EmailAddress.of(query.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundApplicationException(email));
        return UserView.from(user);
    }
}
