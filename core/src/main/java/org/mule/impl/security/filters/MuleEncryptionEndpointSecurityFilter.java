/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.security.filters;

import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.security.AbstractEndpointSecurityFilter;
import org.mule.impl.security.MuleAuthentication;
import org.mule.impl.security.MuleCredentials;
import org.mule.impl.security.MuleHeaderCredentialsAccessor;
import org.mule.umo.UMOEncryptionStrategy;
import org.mule.umo.UMOEvent;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.security.CredentialsNotSetException;
import org.mule.umo.security.CryptoFailureException;
import org.mule.umo.security.EncryptionStrategyNotFoundException;
import org.mule.umo.security.SecurityException;
import org.mule.umo.security.SecurityProviderNotFoundException;
import org.mule.umo.security.UMOAuthentication;
import org.mule.umo.security.UMOCredentials;
import org.mule.umo.security.UMOSecurityContext;
import org.mule.umo.security.UnauthorisedException;
import org.mule.umo.security.UnknownAuthenticationTypeException;

/**
 * <code>MuleEncryptionEndpointSecurityFilter</code> provides password-based
 * encription
 */
public class MuleEncryptionEndpointSecurityFilter extends AbstractEndpointSecurityFilter
{
    private UMOEncryptionStrategy strategy;
    private String strategyName;

    public MuleEncryptionEndpointSecurityFilter()
    {
        setCredentialsAccessor(new MuleHeaderCredentialsAccessor());
    }

    protected final void authenticateInbound(UMOEvent event)
        throws SecurityException, CryptoFailureException, EncryptionStrategyNotFoundException,
        UnknownAuthenticationTypeException
    {
        String userHeader = (String)getCredentialsAccessor().getCredentials(event);
        if (userHeader == null)
        {
            throw new CredentialsNotSetException(event.getMessage(), event.getSession().getSecurityContext(),
                event.getEndpoint(), this);
        }

        UMOCredentials user = new MuleCredentials(userHeader);

        UMOAuthentication authResult;
        UMOAuthentication umoAuthentication = new MuleAuthentication(user);
        try
        {
            authResult = getSecurityManager().authenticate(umoAuthentication);
        }
        catch (Exception e)
        {
            // Authentication failed
            if (logger.isDebugEnabled())
            {
                logger.debug("Authentication request for user: " + user.getUsername() + " failed: "
                             + e.toString());
            }
            throw new UnauthorisedException(new Message(Messages.AUTH_FAILED_FOR_USER_X, user.getUsername()),
                event.getMessage(), e);
        }

        // Authentication success
        if (logger.isDebugEnabled())
        {
            logger.debug("Authentication success: " + authResult.toString());
        }

        UMOSecurityContext context = getSecurityManager().createSecurityContext(authResult);
        context.setAuthentication(authResult);
        event.getSession().setSecurityContext(context);
    }

    protected void authenticateOutbound(UMOEvent event)
        throws SecurityException, SecurityProviderNotFoundException, CryptoFailureException
    {
        if (event.getSession().getSecurityContext() == null)
        {
            if (isAuthenticate())
            {
                throw new UnauthorisedException(event.getMessage(), event.getSession().getSecurityContext(),
                    event.getEndpoint(), this);
            }
            else
            {
                return;
            }
        }
        UMOAuthentication auth = event.getSession().getSecurityContext().getAuthentication();
        if (isAuthenticate())
        {
            auth = getSecurityManager().authenticate(auth);
            if (logger.isDebugEnabled())
            {
                logger.debug("Authentication success: " + auth.toString());
            }
        }

        String token = auth.getCredentials().toString();
        String header = new String(strategy.encrypt(token.getBytes(), null));
        getCredentialsAccessor().setCredentials(event, header);

    }

    protected void doInitialise() throws InitialisationException
    {
        if (strategyName != null)
        {
            strategy = MuleManager.getInstance().getSecurityManager().getEncryptionStrategy(strategyName);
        }

        if (strategy == null)
        {
            throw new InitialisationException(new Message(Messages.ENCRYPT_STRATEGY_NOT_SET), this);
        }
    }

    public UMOEncryptionStrategy getStrategy()
    {
        return strategy;
    }

    public void setStrategy(UMOEncryptionStrategy strategy)
    {
        this.strategy = strategy;
    }

    public void setStrategyName(String name)
    {
        strategyName = name;
    }
}
