package com.app.prod.internal.clients;

import com.app.prod.internal.dtos.ConversationBulkActionDto;
import com.app.prod.internal.dtos.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class RestChattingServiceClient implements ChattingServiceClient{

    private final RestClient restClient;

    public RestChattingServiceClient(
            RestClient.Builder builder,
            @Value("${services.chatting.url}") String baseUrl,
            @Value("${services.chatting.apikey}") String apiKey
    ) {
        this.restClient = builder.baseUrl(baseUrl)
                .defaultHeader(API_KEY_HEADER, apiKey)
                .build();
    }

    @Override
    public void syncUser(UserDto userCreatedDto) {
        restClient.post()
                .uri("/internal/user")
                .contentType(MediaType.APPLICATION_JSON)
                .body(userCreatedDto)
                .retrieve()
                .onStatus(HttpStatusCode::isError, ERROR_HANDLER)
                .toBodilessEntity();

        log.info("Sent notification about new user: {}", userCreatedDto.id());
    }

    @Override
    public void createConversationsForNewUser(ConversationBulkActionDto dto) {
        restClient.post()
                .uri("/internal/bulk-conversation")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .onStatus(HttpStatusCode::isError, ERROR_HANDLER)
                .toBodilessEntity();

        log.info("Sent action - bulk conversation");
    }
}
