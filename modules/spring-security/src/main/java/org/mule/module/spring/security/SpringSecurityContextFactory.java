/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.spring.security;

import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityContext;
import org.mule.api.security.SecurityContextFactory;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

/**
 * <code>SpringSecurityContextFactory</code> creates an SpringSecurityContext for an
 * Authentication object.
 */
public class SpringSecurityContextFactory implements SecurityContextFactory
{
    @Override
    public SecurityContext create(Authentication authentication)
    {
        org.springframework.security.core.context.SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(((SpringAuthenticationAdapter)authentication).getDelegate());

        if (authentication.getProperties() != null)
        {
            if (authentication.getProperties().containsKey("securityMode"))
            {
                String securityMode = (String)authentication.getProperties().get("securityMode");
                SecurityContextHolder.setStrategyName(securityMode);
            }
        }
        SecurityContextHolder.setContext(context);
        return new SpringSecurityContext(context);
    }
}
