package com.menupilot.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
public class HttpsEnforcer extends OncePerRequestFilter {

    @Value("${app.forceHttps:false}")
    private boolean forceHttps;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (forceHttps) {
            String xf = request.getHeader("X-Forwarded-Proto");
            boolean secure = request.isSecure() || (xf != null && xf.equalsIgnoreCase("https"));
            if (!secure) {
                String host = request.getHeader("Host");
                String uri = request.getRequestURI();
                String qs = request.getQueryString();
                String loc = "https://" + (host != null ? host : request.getServerName()) + uri + (qs != null ? "?" + qs : "");
                response.setStatus(301);
                response.setHeader("Location", loc);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
