package com.hrstack.hr_stack.security;

import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final EmployeeRepository employeeRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            EmployeeRepository employeeRepository) {

        this.jwtService = jwtService;
        this.employeeRepository = employeeRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {


        String authHeader =
                request.getHeader("Authorization");

        // No token
        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // Invalid token
        if (!jwtService.isTokenValid(token)) {

            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtService.extractEmail(token);

        Employee employee =
                employeeRepository
                        .findByEmail(email)
                        .orElse(null);

        if (employee == null) {

            filterChain.doFilter(request, response);
            return;
        }

        String role = employee.getRole();

        System.out.println("JWT EMAIL = " + email);
        System.out.println("DB ROLE = " + role);
        System.out.println("AUTHORITY = ROLE_" + role.toUpperCase());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_" +
                                                role.toUpperCase()
                                )
                        )
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);
        System.out.println(
                "AUTHENTICATED = "
                        + SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .isAuthenticated()
        );

        System.out.println(
                "AUTHORITIES = "
                        + SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getAuthorities()
        );

        filterChain.doFilter(request, response);
    }
}