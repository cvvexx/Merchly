package io.cvvexxx.apigateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cloud.gateway.server.mvc.filter.FormFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
public class LocalEndpointFormFilter extends FormFilter {

    static final Set<String> LOCAL_FORM_ENDPOINTS = Set.of("/do-login");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest
                && LOCAL_FORM_ENDPOINTS.contains(httpRequest.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        super.doFilter(request, response, chain);
    }
}
