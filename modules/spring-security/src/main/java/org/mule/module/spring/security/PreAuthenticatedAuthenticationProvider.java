/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.spring.security;

import org.springframework.security.Authentication;
import org.springframework.security.providers.preauth.PreAuthenticatedAuthenticationToken;

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


