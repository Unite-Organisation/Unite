package com.app.prod.event.web;

import com.app.prod.utils.HttpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class RequestSignalsArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return RequestSignals.class.equals(parameter.getParameterType());
    }

    @Override
    public RequestSignals resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            return new RequestSignals(null, null, null);
        }

        return new RequestSignals(
                HttpUtils.getClientIp(request),
                HttpUtils.getUserAgent(request),
                request.getHeader(HttpHeaders.ACCEPT_LANGUAGE)
        );
    }
}
