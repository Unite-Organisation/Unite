package com.app.prod.authorization.service;

import com.app.prod.user.dto.UserActivateRequest;
import com.app.prod.user.repository.UserRepository;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivationService {

    private final UserRepository userRepository;
    private final ActivationTokenService activationTokenService;
    private final BCryptPasswordEncoder encoder;
    private final Validate validate;

    public void activate(UserActivateRequest request){
        UUID userId = activationTokenService.consume(request.token());

        validate.thatUsernameIsFree(request.username());

        String encodedPassword = encoder.encode(request.password());
        userRepository.activateUser(userId, request.username(), encodedPassword, request.firstName(), request.lastName());
        log.info("User {} has been activated as {}", userId, request.username());
    }
}
