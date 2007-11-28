/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.security;

import org.mule.config.i18n.CoreMessages;
import org.mule.impl.ManagementContextAware;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.security.CryptoFailureException;
import org.mule.umo.security.EncryptionStrategyNotFoundException;
import org.mule.umo.security.SecurityException;
import org.mule.umo.security.SecurityProviderNotFoundException;
import org.mule.umo.security.UMOCredentialsAccessor;
import org.mule.umo.security.UMOEndpointSecurityFilter;
import org.mule.umo.security.UMOSecurityManager;
import org.mule.umo.security.UMOSecurityProvider;
import org.mule.umo.security.UnknownAuthenticationTypeException;
import org.mule.util.StringUtils;
import org.mule.transformers.TransformerTemplate;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractEndpointSecurityFilter</code> provides basic initialisation for
 * all security filters, namely configuring the SecurityManager for this instance
 */

public abstract class AbstractEndpointSecurityFilter implements UMOEndpointSecurityFilter, ManagementContextAware
{
    /** logger used by this class */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected UMOSecurityManager securityManager;
    private String securityProviders;
    protected UMOImmutableEndpoint endpoint;
    private boolean inbound = false;
    private boolean authenticate;
    private UMOCredentialsAccessor credentialsAccessor;
    private boolean isInitialised = false;

    protected UMOManagementContext managementContext;

    public void setManagementContext(UMOManagementContext context)
    {
        this.managementContext = context;
    }

    public final void initialise() throws InitialisationException
    {
        if (securityManager == null)
        {
            securityManager = managementContext.getSecurityManager();
        }
        if (securityManager == null)
        {
            throw new InitialisationException(CoreMessages.authSecurityManagerNotSet(), this);
        }

        // This filter may only allow authentication on a subset of registered
        // security providers
        if (securityProviders != null)
        {
            UMOSecurityManager localManager = new MuleSecurityManager();
            String[] sp = StringUtils.splitAndTrim(securityProviders, ",");
            for (int i = 0; i < sp.length; i++)
            {
                UMOSecurityProvider provider = securityManager.getProvider(sp[i]);
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

        // further functionality moved to lazy initialisation
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

        if (endpoint.canRequest())
        {
            inbound = true;
        }
        else if (endpoint.canSend())
        {
            inbound = false;
        }
        else
        {
            throw new InitialisationException(CoreMessages.authEndpointTypeForFilterMustBe(
                UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER + " or " + UMOImmutableEndpoint.ENDPOINT_TYPE_RECEIVER,
                endpoint.getType()), this);
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
    public void setSecurityManager(UMOSecurityManager manager)
    {
        securityManager = manager;
    }

    public UMOSecurityManager getSecurityManager()
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

    public UMOImmutableEndpoint getEndpoint()
    {
        return endpoint;
    }

    public synchronized void setEndpoint(UMOImmutableEndpoint endpoint)
    {
        this.endpoint = endpoint;
        isInitialised = false;
    }

    public void authenticate(UMOEvent event)
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

    public UMOCredentialsAccessor getCredentialsAccessor()
    {
        return credentialsAccessor;
    }

    public void setCredentialsAccessor(UMOCredentialsAccessor credentialsAccessor)
    {
        this.credentialsAccessor = credentialsAccessor;
    }

    protected void updatePayload(UMOMessage message, final Object payload) throws TransformerException
    {
        TransformerTemplate trans = new TransformerTemplate(new TransformerTemplate.TransformerCallback()
        {
            public Object doTransform(UMOMessage message) throws Exception
            {
                return payload;
            }
        });

        message.applyTransformers(Arrays.asList(new Object[]{trans}));
    }

    protected abstract void authenticateInbound(UMOEvent event)
            throws SecurityException, CryptoFailureException, SecurityProviderNotFoundException,
            EncryptionStrategyNotFoundException, UnknownAuthenticationTypeException;

    protected abstract void authenticateOutbound(UMOEvent event)
            throws SecurityException, SecurityProviderNotFoundException, CryptoFailureException;

    protected abstract void doInitialise() throws InitialisationException;

}
