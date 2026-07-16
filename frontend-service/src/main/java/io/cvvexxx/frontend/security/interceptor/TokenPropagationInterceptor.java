package io.cvvexxx.frontend.security.interceptor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TokenPropagationInterceptor implements ClientHttpRequestInterceptor {

    @Override
    @NonNull
    public ClientHttpResponse intercept(
            @NonNull HttpRequest request,
            @NonNull byte[] body,
            @NonNull ClientHttpRequestExecution execution
    ) throws IOException {
        // 1. Достаем текущую аутентификацию из контекста потока
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 2. Проверяем: она должна быть, и это НЕ должен быть анонимный пользователь
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {

            // Вытаскиваем сам токен.
            // В зависимости от того, как ты его сохраняешь при авторизации, это может быть:
            // - authentication.getCredentials() (если при логине клал его туда)
            // - authentication.getPrincipal() (если в кастомном AuthenticationToken токен лежит там)
            Object credentials = authentication.getCredentials();

            if (credentials instanceof String token && !token.isBlank()) {
                request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }
        }

        return execution.execute(request, body);
    }
}