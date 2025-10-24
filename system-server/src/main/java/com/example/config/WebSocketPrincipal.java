package com.example.config;

import java.security.Principal;
import java.util.Objects;

public class WebSocketPrincipal implements Principal {

    private final String name;

    public WebSocketPrincipal(String name) {
        this.name = Objects.requireNonNullElse(name, "anonymous");
    }

    @Override
    public String getName() {
        return name;
    }
}
