/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityContext;
import org.mule.api.security.SecurityContextFactory;
import org.mule.api.security.SecurityProvider;
import org.mule.api.security.UnknownAuthenticationTypeException;

public abstract class AbstractSecurityProvider implements SecurityProvider
{
    private String name;
    private SecurityContextFactory securityContextFactory;

    public AbstractSecurityProvider(String name)
    {
        this.name = name;
    }

    @Override
    public final void initialise() throws InitialisationException
    {
        doInitialise();

        if (securityContextFactory == null)
        {
            securityContextFactory = new DefaultSecurityContextFactory();
        }
    }

    protected void doInitialise() throws InitialisationException
    {
        // do nothing by default
    }

    @Override
    public boolean supports(Class<?> aClass)
    {
        return Authentication.class.isAssignableFrom(aClass);
    }

    @Override
    public SecurityContext createSecurityContext(Authentication authentication)
        throws UnknownAuthenticationTypeException
    {
        return securityContextFactory.create(authentication);
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public void setName(String name)
    {
        this.name = name;
    }

    public SecurityContextFactory getSecurityContextFactory()
    {
        return securityContextFactory;
    }

    public void setSecurityContextFactory(SecurityContextFactory securityContextFactory)
    {
        this.securityContextFactory = securityContextFactory;
    }
}
