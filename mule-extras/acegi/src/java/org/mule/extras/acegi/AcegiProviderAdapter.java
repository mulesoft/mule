/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.acegi;

import net.sf.acegisecurity.Authentication;
import net.sf.acegisecurity.AuthenticationException;
import net.sf.acegisecurity.providers.AuthenticationProvider;
import org.mule.InitialisationException;
import org.mule.umo.security.UMOAuthentication;
import org.mule.umo.security.UMOSecurityException;
import org.mule.umo.security.UMOSecurityProvider;
import org.springframework.beans.factory.InitializingBean;

/**
 * <code>AcegiProviderAdapter</code> is a wrapper for an Acegi Security provider to
 * use with the UMOSecurityManager
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class AcegiProviderAdapter implements UMOSecurityProvider, AuthenticationProvider
{
    private AuthenticationProvider delegate;
    private String name;

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
        try
        {
            if(delegate instanceof InitializingBean) {
                ((InitializingBean)delegate).afterPropertiesSet();
            }
        } catch (Exception e)
        {
            throw new InitialisationException(e.getMessage(), e);
        }
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public UMOAuthentication authenticate(UMOAuthentication authentication) throws UMOSecurityException
    {
        Authentication  auth = delegate.authenticate(((AcegiAuthenticationAdapter)authentication).getDelegate());
        return new AcegiAuthenticationAdapter(auth);
    }

    public Authentication authenticate(Authentication authentication) throws AuthenticationException
    {
        return delegate.authenticate(authentication);
    }

    public boolean supports(Class aClass)
    {
        return aClass.isAssignableFrom(AcegiAuthenticationAdapter.class);
    }
}
