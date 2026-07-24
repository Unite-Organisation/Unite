package com.app.prod.authorization.service;

import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.user.dto.UserActivateRequest;
import com.app.prod.user.repository.UserRepository;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivationService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;
    private final Validate validate;

    public void activate(UserActivateRequest request){
        if(!userRepository.temporaryCredentialsAreValid(request.temporaryLogin(), request.temporaryPassword())){
            throw new BadRequestException(AppError.of(Code.BAD_CREDENTIALS));
        }

        validate.thatUsernameIsFree(request.username());

        String encodedPassword = encoder.encode(request.password());
        userRepository.activateUser(request.temporaryLogin(), request.email(), request.username(), encodedPassword);
        log.info("User {} has been activated", request.username());
    }
}
