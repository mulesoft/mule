/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


