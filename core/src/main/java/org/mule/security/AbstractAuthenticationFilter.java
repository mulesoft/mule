/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.security;

import org.mule.api.MuleEvent;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.AuthenticationFilter;
import org.mule.api.security.CredentialsAccessor;
import org.mule.api.security.CryptoFailureException;
import org.mule.api.security.EncryptionStrategyNotFoundException;
import org.mule.api.security.SecurityException;
import org.mule.api.security.SecurityProviderNotFoundException;
import org.mule.api.security.UnknownAuthenticationTypeException;

/**
 * <code>AbstractEndpointSecurityFilter</code> provides a framework to perform inbound
 * or outbound authentication for messages.
 */
public abstract class AbstractAuthenticationFilter extends AbstractSecurityFilter implements AuthenticationFilter
{
    private boolean authenticate;
    private CredentialsAccessor credentialsAccessor;

    public CredentialsAccessor getCredentialsAccessor()
    {
        return credentialsAccessor;
    }

    public void setCredentialsAccessor(CredentialsAccessor credentialsAccessor)
    {
        this.credentialsAccessor = credentialsAccessor;
    }
    public boolean isAuthenticate()
    {
        return authenticate;
    }

    public void setAuthenticate(boolean authenticate)
    {
        this.authenticate = authenticate;
    }

    @Override
    public void doFilter(MuleEvent event)
        throws SecurityException, UnknownAuthenticationTypeException, CryptoFailureException,
        SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, InitialisationException
    {
        authenticate(event);
    }

    public abstract void authenticate(MuleEvent event)
            throws SecurityException, UnknownAuthenticationTypeException, CryptoFailureException,
            SecurityProviderNotFoundException, EncryptionStrategyNotFoundException,
            InitialisationException;

}
