/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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


