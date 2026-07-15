package io.cvvexxx.frontend.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.WebUtils;

@Configuration
public class ClientBeans {

    private static final Logger log = LoggerFactory.getLogger(ClientBeans.class);

    @Bean
    public ClientHttpRequestInterceptor tokenPropagationInterceptor() {
        return (request, body, execution) -> {
            var attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes servletAttributes) {
                HttpServletRequest currentRequest = servletAttributes.getRequest();
                Cookie cookie = WebUtils.getCookie(currentRequest, "JWT_TOKEN");
                if (cookie != null && !cookie.getValue().isBlank()) {
                    request.getHeaders().setBearerAuth(cookie.getValue());
                    return execution.execute(request, body);
                }
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getCredentials() instanceof String token && !token.isBlank()) {
                request.getHeaders().setBearerAuth(token);
            }

            return execution.execute(request, body);
        };
    }
}