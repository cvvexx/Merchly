package io.cvvexxx.frontend.security;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Getter
public class KeycloakJwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String principal;
    private final String accessToken;

    public KeycloakJwtAuthenticationToken(
            String principal,
            String accessToken,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        this.principal = principal;
        this.accessToken = accessToken;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return accessToken;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

}