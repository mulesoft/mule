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
package org.mule.impl.security.filters;

import org.mule.InitialisationException;
import org.mule.MuleManager;
import org.mule.impl.security.AbstractEndpointSecurityFilter;
import org.mule.impl.security.MuleAuthentication;
import org.mule.impl.security.MuleHeaderCredentialsAccessor;
import org.mule.impl.security.MuleCredentials;
import org.mule.umo.UMOEncryptionStrategy;
import org.mule.umo.UMOEvent;
import org.mule.umo.security.UMOAuthentication;
import org.mule.umo.security.UMOCredentials;
import org.mule.umo.security.UMOSecurityContext;
import org.mule.umo.security.UMOSecurityException;
import org.mule.umo.security.UnauthorisedException;
import org.mule.umo.security.CredentialsNotSetException;

/**
 * <code>MuleEncryptionEndpointSecurityFilter</code> provides password-based encription
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class MuleEncryptionEndpointSecurityFilter extends AbstractEndpointSecurityFilter
{
    private UMOEncryptionStrategy strategy;
    private String strategyName;

    public MuleEncryptionEndpointSecurityFilter()
    {
        setCredentialsAccessor(new MuleHeaderCredentialsAccessor());
    }

    protected final void authenticateInbound(UMOEvent event) throws UMOSecurityException
    {
        String userHeader = (String)getCredentialsAccessor().getCredentials(event);
        if (userHeader == null)
        {
            throw new CredentialsNotSetException(event.getSession().getSecurityContext(), event.getEndpoint(), this);
        }
        byte[] creds = null;
        if(userHeader.startsWith("Plain ")) {
            creds = userHeader.substring(6).getBytes();
        } else {
            creds = strategy.decrypt(userHeader.getBytes());
        }
        UMOCredentials user = new MuleCredentials(new String(creds));

        UMOAuthentication authResult;
        UMOAuthentication umoAuthentication = new MuleAuthentication(user);
        try
        {
            authResult = getSecurityManager().authenticate(umoAuthentication);
        } catch (UMOSecurityException e)
        {
            // Authentication failed
            if (logger.isDebugEnabled())
            {
                logger.debug("Authentication request for user: " + user.getUsername()
                        + " failed: " + e.toString());
            }
            throw new UnauthorisedException("Authentication failed for " + user.getUsername() + ": " + e.getMessage(), e);
        }

        // Authentication success
        if (logger.isDebugEnabled())
        {
            logger.debug("Authentication success: " + authResult.toString());
        }

        UMOSecurityContext context = getSecurityManager().createSecurityContext(authResult);
        event.getSession().setSecurityContext(context);
    }

    protected void authenticateOutbound(UMOEvent event) throws UMOSecurityException
    {
        if (event.getSession().getSecurityContext() == null)
        {
            if (isAuthenticate())
            {
                throw new UnauthorisedException(event.getSession().getSecurityContext(), event.getEndpoint(), this);
            } else
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
        String header = new String(strategy.encrypt(token.getBytes()));
        getCredentialsAccessor().setCredentials(event, header);

    }

    protected void doInitialise() throws InitialisationException
    {
        if(strategyName!=null) {
            strategy = MuleManager.getInstance().getSecurityManager().getEncryptionStrategy(strategyName);
        }

        if (strategy == null)
        {
            throw new InitialisationException("No encryption strategy has been set on this filter");
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
