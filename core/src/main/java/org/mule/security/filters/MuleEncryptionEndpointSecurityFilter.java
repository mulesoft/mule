/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.filters;

import org.mule.api.EncryptionStrategy;
import org.mule.api.MuleEvent;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.Authentication;
import org.mule.api.security.Credentials;
import org.mule.api.security.CredentialsNotSetException;
import org.mule.api.security.CryptoFailureException;
import org.mule.api.security.EncryptionStrategyNotFoundException;
import org.mule.api.security.SecurityContext;
import org.mule.api.security.SecurityException;
import org.mule.api.security.SecurityProviderNotFoundException;
import org.mule.api.security.UnauthorisedException;
import org.mule.api.security.UnknownAuthenticationTypeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.security.AbstractOperationSecurityFilter;
import org.mule.security.DefaultMuleAuthentication;
import org.mule.security.MuleCredentials;
import org.mule.security.MuleHeaderCredentialsAccessor;

/**
 * <code>MuleEncryptionEndpointSecurityFilter</code> provides password-based
 * encryption
 */
public class MuleEncryptionEndpointSecurityFilter extends AbstractOperationSecurityFilter
{
    private EncryptionStrategy strategy;

    public MuleEncryptionEndpointSecurityFilter()
    {
        setCredentialsAccessor(new MuleHeaderCredentialsAccessor());
    }

    @Override
    protected void authenticateInbound(MuleEvent event)
            throws SecurityException, SecurityProviderNotFoundException, CryptoFailureException, EncryptionStrategyNotFoundException, UnknownAuthenticationTypeException
    {
        String userHeader = (String) getCredentialsAccessor().getCredentials(event);
        if (userHeader == null)
        {
            throw new CredentialsNotSetException(event, event.getSession().getSecurityContext(), this);
        }

        Credentials user = new MuleCredentials(userHeader, getSecurityManager());

        Authentication authentication;
        try
        {
            authentication = getSecurityManager().authenticate(new DefaultMuleAuthentication(user, event));
        }
        catch (Exception e)
        {
            // Authentication failed
            if (logger.isDebugEnabled())
            {
                logger.debug("Authentication request for user: " + user.getUsername()
                             + " failed: " + e.toString());
            }
            throw new UnauthorisedException(
                    CoreMessages.authFailedForUser(user.getUsername()), event, e);
        }

        // Authentication success
        if (logger.isDebugEnabled())
        {
            logger.debug("Authentication success: " + authentication.toString());
        }

        SecurityContext context = getSecurityManager().createSecurityContext(authentication);
        context.setAuthentication(authentication);
        event.getSession().setSecurityContext(context);
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        if (strategy == null)
        {
            throw new InitialisationException(CoreMessages.encryptionStrategyNotSet(), this);
        }
    }

    public EncryptionStrategy getStrategy()
    {
        return strategy;
    }

    public void setStrategy(EncryptionStrategy strategy)
    {
        this.strategy = strategy;
    }

}
