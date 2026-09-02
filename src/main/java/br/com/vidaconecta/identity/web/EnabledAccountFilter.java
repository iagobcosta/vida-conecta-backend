package br.com.vidaconecta.identity.web;

import br.com.vidaconecta.identity.api.CurrentUser;
import br.com.vidaconecta.identity.infrastructure.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class EnabledAccountFilter extends OncePerRequestFilter {

	private final UserRepository userRepository;

	public EnabledAccountFilter(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof CurrentUser currentUser) {
			boolean enabled = userRepository.findById(currentUser.id())
					.map(user -> user.isEnabled())
					.orElse(false);
			if (!enabled) {
				SecurityContextHolder.clearContext();
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.setContentType(MediaType.APPLICATION_JSON_VALUE);
				response.getWriter().write("""
						{"status":401,"error":"Unauthorized","message":"Esta conta foi desativada"}
						""");
				return;
			}
		}
		filterChain.doFilter(request, response);
	}
}
