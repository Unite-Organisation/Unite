package com.app.prod.access.web;

import com.app.prod.access.BuildingAccessService;
import com.app.prod.access.BuildingScope;
import com.app.prod.config.security.GlobalSecurityManager;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BuildingScopeArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String BUILDING_ID_PARAM = "buildingId";
    private final ObjectProvider<GlobalSecurityManager> globalSecurityManager;
    private final BuildingAccessService buildingAccessService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return BuildingScope.class.equals(parameter.getParameterType());
    }

    @Override
    public BuildingScope resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                         NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        UUID buildingId = readBuildingId(webRequest);
        GlobalSecurityManager securityManager = globalSecurityManager.getObject();
        return buildingAccessService.authorize(securityManager.getCurrentUser(), securityManager.getUserRole(), buildingId);
    }

    private UUID readBuildingId(NativeWebRequest webRequest) {
        String rawBuildingId = webRequest.getParameter(BUILDING_ID_PARAM);
        if (rawBuildingId == null || rawBuildingId.isBlank()) {
            throw new BadRequestException(AppError.of(Code.BUILDING_ID_REQUIRED));
        }

        try {
            return UUID.fromString(rawBuildingId);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(AppError.of(
                    Code.BUILDING_ID_REQUIRED,
                    String.format("%s is not a valid building id", rawBuildingId)
            ));
        }
    }
}
