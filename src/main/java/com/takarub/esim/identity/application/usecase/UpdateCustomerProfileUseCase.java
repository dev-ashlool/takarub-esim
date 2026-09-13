package com.takarub.esim.identity.application.usecase;

import com.takarub.esim.identity.application.command.UpdateCustomerProfileCommand;
import com.takarub.esim.identity.application.exception.UserNotFoundApplicationException;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.application.result.UserView;
import com.takarub.esim.identity.domain.user.User;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.domain.user.UserRepository;
import com.takarub.esim.identity.shared.time.ClockProvider;

public class UpdateCustomerProfileUseCase {

    private final TransactionRunner transactionRunner;
    private final UserRepository userRepository;
    private final ClockProvider clock;

    public UpdateCustomerProfileUseCase(TransactionRunner transactionRunner,
                                        UserRepository userRepository,
                                        ClockProvider clock) {
        this.transactionRunner = transactionRunner;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public UserView execute(UpdateCustomerProfileCommand command) {
        return transactionRunner.execute(() -> {
            UserId userId = UserId.of(command.userId());
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundApplicationException(userId));
            user.updateProfile(command.fullName(), command.country(), command.phoneNumber(), clock);
            userRepository.save(user);
            return UserView.from(user);
        });
    }
}
