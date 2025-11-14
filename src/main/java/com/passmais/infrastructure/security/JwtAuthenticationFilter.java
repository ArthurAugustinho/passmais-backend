package com.passmais.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.passmais.infrastructure.repository.RevokedTokenRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final RevokedTokenRepository revokedTokenRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService, RevokedTokenRepository revokedTokenRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.revokedTokenRepository = revokedTokenRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Ignora endpoints públicos configurados como permitAll()
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/api/auth/") || requestURI.startsWith("/api/registration/") || 
            requestURI.startsWith("/v3/api-docs/") || requestURI.contains("/swagger-ui")) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                if (jwtService.isValid(token)) {
                    // Bloquear token se revogado e ainda não expirado
                    boolean isRevoked = revokedTokenRepository
                            .findFirstByTokenAndExpiresAtAfter(token, java.time.Instant.now())
                            .isPresent();
                    if (isRevoked) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token revogado");
                        return;
                    }
                    String username = jwtService.getSubject(token);
                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (Exception e) {
                // Loga a exceção e continua a cadeia para evitar bloqueio total
                System.err.println("Erro ao processar token JWT: " + e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}