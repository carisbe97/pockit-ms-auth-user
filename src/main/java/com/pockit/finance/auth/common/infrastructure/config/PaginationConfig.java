package com.pockit.finance.auth.common.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class PaginationConfig implements PageableHandlerMethodArgumentResolverCustomizer {

    @Override
    public void customize(PageableHandlerMethodArgumentResolver resolver) {
        resolver.setOneIndexedParameters(false);
        resolver.setMaxPageSize(100);
        resolver.setFallbackPageable(PageRequest.of(0, 10));
    }
}
