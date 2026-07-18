package com.roomfit.client;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ClientScopeInterceptor implements HandlerInterceptor {

    private final ClientScopeService clientScopeService;
    private final ClientScopeContext clientScopeContext;

    public ClientScopeInterceptor(ClientScopeService clientScopeService, ClientScopeContext clientScopeContext) {
        this.clientScopeService = clientScopeService;
        this.clientScopeContext = clientScopeContext;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Browser CORS preflight does not carry application headers.
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        clientScopeContext.set(clientScopeService.resolve(request.getHeader(ClientScopeService.HEADER_NAME)));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        clientScopeContext.clear();
    }
}
