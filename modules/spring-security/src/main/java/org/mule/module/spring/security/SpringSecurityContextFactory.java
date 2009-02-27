/*
 * $Id: AcegiSecurityContextFactory.java 10662 2008-02-01 13:10:14Z romikk $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.spring.security;

import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityContext;
import org.mule.api.security.SecurityContextFactory;

import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.context.SecurityContextImpl;

/**
 * <code>AcegiSecurityContextFactory</code> creates an AcegiSecurityContext for an
 * Authentication object
 */
public class SpringSecurityContextFactory implements SecurityContextFactory
{
    public SecurityContext create(Authentication authentication)
    {
        org.springframework.security.context.SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(((SpringAuthenticationAdapter)authentication).getDelegate());

        if (authentication.getProperties() != null)
        {
            if ((authentication.getProperties().containsKey("securityMode")))
            {
                SecurityContextHolder.setStrategyName((String)authentication.getProperties().get(
                    "securityMode"));
            }
        }
        SecurityContextHolder.setContext(context);
        return new SpringSecurityContext(context);
    }
}
