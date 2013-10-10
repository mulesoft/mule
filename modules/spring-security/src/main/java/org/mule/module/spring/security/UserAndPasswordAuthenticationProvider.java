/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.spring.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * A spring authentication provider that return 
 * 
 * @author estebanroblesluna
 */
public class UserAndPasswordAuthenticationProvider implements SpringAuthenticationProvider
{
    /**
     * {@inheritDoc}
     */
    public Authentication getAuthentication(org.mule.api.security.Authentication authentication)
    {
        return new UsernamePasswordAuthenticationToken(authentication.getPrincipal(),
            authentication.getCredentials());
    }
}


