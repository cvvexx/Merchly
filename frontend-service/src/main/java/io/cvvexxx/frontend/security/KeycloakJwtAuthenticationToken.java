package io.cvvexxx.frontend.security;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serializable;
import java.util.Collection;
import java.util.UUID;

@Getter
public class KeycloakJwtAuthenticationToken extends AbstractAuthenticationToken implements Serializable {

    private static final long serialVersionUID = 2L;

    private final String principal;
    private final UUID userId;
    private final String accessToken;

    public KeycloakJwtAuthenticationToken(
            String principal,
            UUID userId,
            String accessToken,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        this.principal = principal;
        this.userId = userId;
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
