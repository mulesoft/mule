/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.acegi;

import net.sf.acegisecurity.Authentication;
import net.sf.acegisecurity.AuthenticationException;
import net.sf.acegisecurity.providers.AuthenticationProvider;
import net.sf.acegisecurity.providers.UsernamePasswordAuthenticationToken;

import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.security.SecurityException;
import org.mule.umo.security.UMOAuthentication;
import org.mule.umo.security.UMOSecurityContext;
import org.mule.umo.security.UMOSecurityContextFactory;
import org.mule.umo.security.UMOSecurityProvider;
import org.mule.umo.security.UnknownAuthenticationTypeException;

/**
 * <code>AcegiProviderAdapter</code> is a wrapper for an Acegi Security
 * provider to use with the UMOSecurityManager
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class AcegiProviderAdapter implements UMOSecurityProvider, AuthenticationProvider
{
    private AuthenticationProvider delegate;
    private String name;
    private UMOSecurityContextFactory factory;

    public AcegiProviderAdapter()
    {
        super();
    }

    public AcegiProviderAdapter(AuthenticationProvider delegate)
    {
        this.delegate = delegate;
    }

    public AcegiProviderAdapter(AuthenticationProvider delegate, String name)
    {
        this.delegate = delegate;
        this.name = name;
    }

    public void initialise() throws InitialisationException
    {
        // //all initialisation should be handled in the spring
        // intitialisation hook afterPropertiesSet()

        // register context factory
        factory = new AcegiSecurityContextFactory();
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public UMOAuthentication authenticate(UMOAuthentication authentication) throws SecurityException
    {
        Authentication auth = null;
        if (authentication instanceof AcegiAuthenticationAdapter) {
            auth = ((AcegiAuthenticationAdapter) authentication).getDelegate();
        } else {
            auth = new UsernamePasswordAuthenticationToken(authentication.getPrincipal(),
                                                           authentication.getCredentials());
        }

        auth = delegate.authenticate(auth);
        return new AcegiAuthenticationAdapter(auth);
    }

    public Authentication authenticate(Authentication authentication) throws AuthenticationException
    {
        return delegate.authenticate(authentication);
    }

    public boolean supports(Class aClass)
    {
        return UMOAuthentication.class.isAssignableFrom(aClass);
    }

    public AuthenticationProvider getDelegate()
    {
        return delegate;
    }

    public void setDelegate(AuthenticationProvider delegate)
    {
        this.delegate = delegate;
    }

    public UMOSecurityContext createSecurityContext(UMOAuthentication auth) throws UnknownAuthenticationTypeException
    {
        return factory.create(auth);
    }
}
