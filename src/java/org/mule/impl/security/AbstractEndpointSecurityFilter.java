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
package org.mule.impl.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.UMOEvent;
import org.mule.umo.endpoint.UMOEndpoint;
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
import org.mule.util.Utility;

/**
 * <code>AbstractEndpointSecurityFilter</code> provides basic initialisation
 * for all security filters, namely configuring the SecurityManager for this
 * instance
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public abstract class AbstractEndpointSecurityFilter implements UMOEndpointSecurityFilter
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    private UMOSecurityManager securityManager;
    private String securityProviders;
    private UMOImmutableEndpoint endpoint;
    private boolean inbound = false;
    private boolean authenticate;
    private UMOCredentialsAccessor credentialsAccessor;

    public final void initialise() throws InitialisationException
    {
        if (securityManager == null) {
            securityManager = MuleManager.getInstance().getSecurityManager();
        }
        if (securityManager == null) {
            throw new InitialisationException(new Message(Messages.AUTH_SECURITY_MANAGER_NOT_SET), this);
        }
        if (endpoint == null) {
            throw new InitialisationException(new Message(Messages.X_IS_NULL, "Endpoint"), this);
        }
        // This filter may only allow authentication on a subset of registered
        // security providers
        if (securityProviders != null) {
            UMOSecurityManager localManager = new MuleSecurityManager();
            String[] sp = Utility.split(securityProviders, ",");
            for (int i = 0; i < sp.length; i++) {
                UMOSecurityProvider provider = securityManager.getProvider(sp[i]);
                if (provider != null) {
                    localManager.addProvider(provider);
                } else {
                    throw new InitialisationException(new Message(Messages.X_NOT_REGISTERED_WITH_MANAGER,
                                                                  "Security Provider '" + sp[i] + "'"), this);
                }
            }
            securityManager = localManager;
        }
        if (endpoint.getType().equals(UMOEndpoint.ENDPOINT_TYPE_RECEIVER)) {
            inbound = true;
        } else if (endpoint.getType().equals(UMOEndpoint.ENDPOINT_TYPE_SENDER)) {
            inbound = false;
        } else {
            throw new InitialisationException(new Message(Messages.AUTH_ENDPOINT_TYPE_FOR_FILTER_MUST_BE_X_BUT_IS_X,
                                                          UMOEndpoint.ENDPOINT_TYPE_SENDER + " or "
                                                                  + UMOEndpoint.ENDPOINT_TYPE_RECEIVER,
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

    /**
     * @param manager
     */
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

    public void setEndpoint(UMOImmutableEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    public void authenticate(UMOEvent event) throws SecurityException, UnknownAuthenticationTypeException,
            CryptoFailureException, SecurityProviderNotFoundException, EncryptionStrategyNotFoundException
    {
        if (inbound) {
            authenticateInbound(event);
        } else {
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

    protected abstract void authenticateInbound(UMOEvent event) throws SecurityException, CryptoFailureException,
            SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, UnknownAuthenticationTypeException;

    protected abstract void authenticateOutbound(UMOEvent event) throws SecurityException,
            SecurityProviderNotFoundException, CryptoFailureException;

    protected abstract void doInitialise() throws InitialisationException;

}
