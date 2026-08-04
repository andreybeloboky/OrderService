package com.beloboki.config;

import java.util.Collection;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CustomJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String USER_ID = "userId";
    private static final String ROLE = "role";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String username = jwt.getSubject();
        Long userId = jwt.getClaim(USER_ID);
        String role = jwt.getClaim(ROLE);

        CurrentUser currentUser = new CurrentUser(userId, username, role);
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new UsernamePasswordAuthenticationToken(
                currentUser, jwt.getTokenValue(), authorities);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        String role = jwt.getClaim(ROLE);
        if (role == null) return List.of();

        return List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
    }
}
