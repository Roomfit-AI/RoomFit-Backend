package com.roomfit.client;

import org.springframework.stereotype.Component;

@Component
public class ClientScopeContext {

    private final ThreadLocal<ClientScope> scopeHolder = new ThreadLocal<>();

    public ClientScope current() {
        ClientScope scope = scopeHolder.get();
        return scope == null ? ClientScope.legacyScope() : scope;
    }

    public void set(ClientScope scope) {
        scopeHolder.set(scope);
    }

    public void clear() {
        scopeHolder.remove();
    }
}
