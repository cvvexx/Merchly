package io.cvvexxx.apigateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class TokenRelayFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String bearer = null;
        if (authentication instanceof KeycloakJwtAuthenticationToken token && token.getAccessToken() != null) {
            bearer = "Bearer " + token.getAccessToken();
        }

        filterChain.doFilter(new BearerRequestWrapper(request, bearer), response);
    }

    private static final class BearerRequestWrapper extends HttpServletRequestWrapper {

        private final String bearer;

        private BearerRequestWrapper(HttpServletRequest request, String bearer) {
            super(request);
            this.bearer = bearer;
        }

        @Override
        public String getHeader(String name) {
            if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                return bearer;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                return bearer == null
                        ? Collections.emptyEnumeration()
                        : Collections.enumeration(List.of(bearer));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = new ArrayList<>();
            Enumeration<String> original = super.getHeaderNames();
            while (original != null && original.hasMoreElements()) {
                String name = original.nextElement();
                if (!HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                    names.add(name);
                }
            }
            if (bearer != null) {
                names.add(HttpHeaders.AUTHORIZATION);
            }
            return Collections.enumeration(names);
        }
    }
}
