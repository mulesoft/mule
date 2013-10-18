/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.security;

import org.mule.api.security.Authentication;

/**
 * A provider for spring authentication
 * 
 * @author estebanroblesluna
 */
public interface SpringAuthenticationProvider
{

    /**
     * Provides a spring authentication according to mule's authentication
     * 
     * @param authentication the mule's authentication
     * @return the spring's authentication
     */
    org.springframework.security.core.Authentication getAuthentication(Authentication authentication);    
}


