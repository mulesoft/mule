/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.spring.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;


/**
 * Provides a pre authenticated authentication
 * 
 * @author estebanroblesluna
 */
public class PreAuthenticatedAuthenticationProvider implements SpringAuthenticationProvider
{
    /**
     * {@inheritDoc}
     */
    public Authentication getAuthentication(org.mule.api.security.Authentication authentication)
    {
        return new PreAuthenticatedAuthenticationToken(authentication.getPrincipal(),
            authentication.getCredentials());
    }
}


