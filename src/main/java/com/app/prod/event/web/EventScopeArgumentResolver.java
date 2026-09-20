package com.app.prod.event.web;

import com.app.prod.event.dto.SessionMember;
import com.app.prod.event.enums.EventIdentityOrigin;
import com.app.prod.event.service.EventSessionService;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.AuthenticationFailedException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EventScopeArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String SLUG_VARIABLE = "slug";

    private final EventSessionService eventSessionService;
    private final Clock clock;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return EventScope.class.equals(parameter.nestedIfOptional().getNestedParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        Optional<EventScope> scope = Optional.ofNullable(request)
                .flatMap(this::resolve);

        if (parameter.isOptional()) {
            return scope;
        }
        return scope.orElseThrow(() -> new AuthenticationFailedException(AppError.of(Code.EVENT_SESSION_REQUIRED)));
    }

    private Optional<EventScope> resolve(HttpServletRequest request) {
        Optional<String> slug = slug(request);
        Optional<String> token = sessionToken(request);
        if (slug.isEmpty() || token.isEmpty()) {
            return Optional.empty();
        }

        return eventSessionService.findActive(slug.get(), token.get(), LocalDateTime.now(clock))
                .map(EventScopeArgumentResolver::toScope);
    }

    private static EventScope toScope(SessionMember member) {
        EventIdentityOrigin origin = member.userId() != null ? EventIdentityOrigin.UNITE : EventIdentityOrigin.GUEST;
        return new EventScope(member.eventId(), member.memberId(), member.role(), origin);
    }

    @SuppressWarnings("unchecked")
    private static Optional<String> slug(HttpServletRequest request) {
        Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(variables instanceof Map<?, ?> map)) {
            return Optional.empty();
        }
        return Optional.ofNullable((String) ((Map<String, String>) map).get(SLUG_VARIABLE));
    }

    private static Optional<String> sessionToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> EventSessionService.COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }
}
