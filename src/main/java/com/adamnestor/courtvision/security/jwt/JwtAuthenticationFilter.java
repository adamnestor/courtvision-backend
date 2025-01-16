package com.adamnestor.courtvision.security.jwt;

import com.adamnestor.courtvision.security.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    protected final JwtTokenUtil jwtTokenUtil;
    protected final UserDetailsServiceImpl userDetailsService;

    public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil, UserDetailsServiceImpl userDetailsService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            logger.debug("Processing request: {} {}", request.getMethod(), request.getRequestURI());

            // Check if it's a public endpoint
            if (request.getRequestURI().startsWith("/api/public/") ||
                    request.getRequestURI().startsWith("/api/auth/")) {
                logger.debug("Public endpoint, skipping authentication");
                filterChain.doFilter(request, response);
                return;
            }

            String jwt = getJwtFromRequest(request);
            logger.debug("JWT from request: {}", jwt != null ? "present" : "missing");

            // If no token is present for protected endpoints
            if (!StringUtils.hasText(jwt)) {
                logger.warn("No JWT token found in request");
                filterChain.doFilter(request, response);
                return;
            }

            // If token is invalid
            if (!jwtTokenUtil.validateToken(jwt)) {
                logger.warn("Invalid JWT token");
                filterChain.doFilter(request, response);
                return;
            }

            String email = jwtTokenUtil.getEmailFromToken(jwt);
            logger.debug("Email extracted from token: {}", email);

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            logger.debug("User details loaded: {}", userDetails != null ? "success" : "failed");

            // Check for null userDetails
            if (userDetails == null) {
                logger.warn("User details not found for email: {}", email);
                filterChain.doFilter(request, response);
                return;
            }

            // Additional validation with UserDetails
            if (!jwtTokenUtil.validateToken(jwt, userDetails)) {
                logger.warn("JWT token validation failed for user: {}", email);
                filterChain.doFilter(request, response);
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            logger.debug("Authentication set in SecurityContext for user: {}", email);

            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
            filterChain.doFilter(request, response);
        }
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}