/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.security;

import org.mule.api.NameableObject;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityContext;
import org.mule.api.security.SecurityContextFactory;
import org.mule.api.security.SecurityProvider;
import org.mule.api.security.UnknownAuthenticationTypeException;


public abstract class AbstractSecurityProvider implements SecurityProvider, NameableObject
{
    private String name;
    private SecurityContextFactory securityContextFactory;

    public AbstractSecurityProvider(String name)
    {
        this.name = name;
    }

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
    
    public boolean supports(Class aClass)
    {
        return Authentication.class.isAssignableFrom(aClass);
    }

    public SecurityContext createSecurityContext(Authentication authentication)
        throws UnknownAuthenticationTypeException
    {
        return securityContextFactory.create(authentication);
    }

    public String getName()
    {
        return name;
    }

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
