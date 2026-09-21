package com.springexample.webchat.config;

import lombok.RequiredArgsConstructor;

import java.security.Principal;

@RequiredArgsConstructor
public class DefaultHandshakePrincipal implements Principal {

    private final String name;

    @Override
    public String getName() {
        return name;
    }
}
