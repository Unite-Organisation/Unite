package com.app.prod.internal.clients;

import com.app.prod.internal.dtos.UserDto;

public interface ChattingServiceClient extends InternalApiClient{
    void syncUser(UserDto userCreatedDto);
}
