package br.com.vidaconecta.identity.web;

import br.com.vidaconecta.identity.api.CurrentUser;
import br.com.vidaconecta.identity.api.Role;
import java.util.List;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtCurrentUserConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		Role role = Role.valueOf(jwt.getClaimAsString("role"));
		CurrentUser user = new CurrentUser(
				UUID.fromString(jwt.getSubject()),
				jwt.getClaimAsString("email"),
				role);
		var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
		return new UsernamePasswordAuthenticationToken(user, jwt, authorities);
	}
}
