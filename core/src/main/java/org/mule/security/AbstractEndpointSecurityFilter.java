/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.security;

import org.mule.api.MuleEvent;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.CryptoFailureException;
import org.mule.api.security.EncryptionStrategyNotFoundException;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.security.SecurityException;
import org.mule.api.security.SecurityProviderNotFoundException;
import org.mule.api.security.UnknownAuthenticationTypeException;

/**
 * <code>AbstractEndpointSecurityFilter</code> provides basic initialisation for all security filters, namely
 * configuring the SecurityManager for this instance
 */
@Deprecated
public abstract class AbstractEndpointSecurityFilter extends AbstractAuthenticationFilter
    implements EndpointSecurityFilter
{
    protected ImmutableEndpoint endpoint;

    public ImmutableEndpoint getEndpoint()
    {
        return endpoint;
    }

    public synchronized void setEndpoint(ImmutableEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    @Override
    public void doFilter(MuleEvent event)
        throws SecurityException, UnknownAuthenticationTypeException, CryptoFailureException,
        SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, InitialisationException
    {
        super.doFilter(event);
    }

    public void authenticate(MuleEvent event)
        throws SecurityException, UnknownAuthenticationTypeException, CryptoFailureException,
        SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, InitialisationException
    {
        if (endpoint == null || endpoint instanceof InboundEndpoint)
        {
            authenticateInbound(event);
        }
        else
        {
            authenticateOutbound(event);
        }
    }

    protected abstract void authenticateInbound(MuleEvent event)
        throws SecurityException, CryptoFailureException, SecurityProviderNotFoundException,
        EncryptionStrategyNotFoundException, UnknownAuthenticationTypeException;

    protected abstract void authenticateOutbound(MuleEvent event)
        throws SecurityException, SecurityProviderNotFoundException, CryptoFailureException;

}
