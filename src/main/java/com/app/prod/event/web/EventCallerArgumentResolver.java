package com.app.prod.event.web;

import com.app.prod.config.security.GlobalSecurityManager;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.AuthenticationFailedException;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class EventCallerArgumentResolver implements HandlerMethodArgumentResolver {

    private final ObjectProvider<GlobalSecurityManager> globalSecurityManager;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return EventCaller.class.equals(parameter.getParameterType());
    }

    @Override
    public EventCaller resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                       NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        if (!isAuthenticated()) {
            if (webRequest.getHeader(HttpHeaders.AUTHORIZATION) != null) {
                throw new AuthenticationFailedException(AppError.of(Code.INVALID_AUTHORIZATION_TOKEN));
            }
            return new EventCaller(null);
        }

        AppUserRecord user = globalSecurityManager.getObject().getCurrentUser();
        if (user == null) {
            throw new AuthenticationFailedException(AppError.of(Code.INVALID_AUTHORIZATION_TOKEN));
        }
        return new EventCaller(user);
    }

    private static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }
}
