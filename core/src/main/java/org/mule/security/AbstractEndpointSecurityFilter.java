/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.CredentialsAccessor;
import org.mule.api.security.CryptoFailureException;
import org.mule.api.security.EncryptionStrategyNotFoundException;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.security.SecurityException;
import org.mule.api.security.SecurityManager;
import org.mule.api.security.SecurityProvider;
import org.mule.api.security.SecurityProviderNotFoundException;
import org.mule.api.security.UnknownAuthenticationTypeException;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.TransformerTemplate;
import org.mule.util.StringUtils;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractEndpointSecurityFilter</code> provides basic initialisation for
 * all security filters, namely configuring the SecurityManager for this instance
 */

public abstract class AbstractEndpointSecurityFilter implements EndpointSecurityFilter, MuleContextAware
{

    protected transient Log logger = LogFactory.getLog(getClass());

    protected SecurityManager securityManager;
    private String securityProviders;
    protected ImmutableEndpoint endpoint;
    private boolean inbound = false;
    private boolean authenticate;
    private CredentialsAccessor credentialsAccessor;
    private boolean isInitialised = false;

    protected MuleContext muleContext;

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public final void initialise() throws InitialisationException
    {
        if (securityManager == null)
        {
            securityManager = muleContext.getSecurityManager();
        }
        if (securityManager == null)
        {
            throw new InitialisationException(CoreMessages.authSecurityManagerNotSet(), this);
        }

        // This filter may only allow authentication on a subset of registered
        // security providers
        if (securityProviders != null)
        {
            SecurityManager localManager = new MuleSecurityManager();
            String[] sp = StringUtils.splitAndTrim(securityProviders, ",");
            for (int i = 0; i < sp.length; i++)
            {
                SecurityProvider provider = securityManager.getProvider(sp[i]);
                if (provider != null)
                {
                    localManager.addProvider(provider);
                }
                else
                {
                    throw new InitialisationException(
                            CoreMessages.objectNotRegistered(
                                    "Security Provider", sp[i]), this);
                }
            }
            securityManager = localManager;
        }
    }

    protected final synchronized void lazyInit() throws InitialisationException
    {
        if (!isInitialised)
        {
            initialiseEndpoint();
            isInitialised = true;
        }
    }

    protected final void initialiseEndpoint() throws InitialisationException
    {
        if (endpoint == null)
        {
            throw new InitialisationException(CoreMessages.objectIsNull("Endpoint"), this);
        }

        if (endpoint instanceof InboundEndpoint)
        {
            inbound = true;
        }
        else if (endpoint instanceof OutboundEndpoint)
        {
            inbound = false;
        }
        else
        {
            throw new InitialisationException(CoreMessages.authEndpointMustSendOrReceive(), this);
        }
        doInitialise();
    }

    public boolean isAuthenticate()
    {
        return authenticate;
    }

    public void setAuthenticate(boolean authenticate)
    {
        this.authenticate = authenticate;
    }

    /** @param manager  */
    public void setSecurityManager(SecurityManager manager)
    {
        securityManager = manager;
    }

    public SecurityManager getSecurityManager()
    {
        return securityManager;
    }

    public String getSecurityProviders()
    {
        return securityProviders;
    }

    public void setSecurityProviders(String providers)
    {
        securityProviders = providers;
    }

    public ImmutableEndpoint getEndpoint()
    {
        return endpoint;
    }

    public synchronized void setEndpoint(ImmutableEndpoint endpoint)
    {
        this.endpoint = endpoint;
        isInitialised = false;
    }

    public void authenticate(MuleEvent event)
            throws SecurityException, UnknownAuthenticationTypeException, CryptoFailureException,
            SecurityProviderNotFoundException, EncryptionStrategyNotFoundException,
            InitialisationException
    {
        lazyInit();
        if (inbound)
        {
            authenticateInbound(event);
        }
        else
        {
            authenticateOutbound(event);
        }
    }

    public CredentialsAccessor getCredentialsAccessor()
    {
        return credentialsAccessor;
    }

    public void setCredentialsAccessor(CredentialsAccessor credentialsAccessor)
    {
        this.credentialsAccessor = credentialsAccessor;
    }

    protected void updatePayload(MuleMessage message, final Object payload) throws TransformerException
    {
        TransformerTemplate trans = new TransformerTemplate(new TransformerTemplate.TransformerCallback()
        {
            public Object doTransform(MuleMessage message) throws Exception
            {
                return payload;
            }
        });

        message.applyTransformers(Arrays.asList(trans));
    }

    protected abstract void authenticateInbound(MuleEvent event)
            throws SecurityException, CryptoFailureException, SecurityProviderNotFoundException,
            EncryptionStrategyNotFoundException, UnknownAuthenticationTypeException;

    protected abstract void authenticateOutbound(MuleEvent event)
            throws SecurityException, SecurityProviderNotFoundException, CryptoFailureException;

    protected abstract void doInitialise() throws InitialisationException;

}
