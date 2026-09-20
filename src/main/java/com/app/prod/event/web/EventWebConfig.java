package com.app.prod.event.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class EventWebConfig implements WebMvcConfigurer {

    private final EventCallerArgumentResolver eventCallerArgumentResolver;
    private final EventScopeArgumentResolver eventScopeArgumentResolver;
    private final RequestSignalsArgumentResolver requestSignalsArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(eventCallerArgumentResolver);
        resolvers.add(eventScopeArgumentResolver);
        resolvers.add(requestSignalsArgumentResolver);
    }
}
