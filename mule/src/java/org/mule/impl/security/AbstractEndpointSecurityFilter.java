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
package org.mule.impl.security;

import org.mule.InitialisationException;
import org.mule.MuleManager;
import org.mule.umo.UMOEvent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.security.UMOEndpointSecurityFilter;
import org.mule.umo.security.UMOSecurityException;
import org.mule.umo.security.UMOSecurityManager;
import org.mule.umo.security.UMOSecurityProvider;
import org.mule.util.Utility;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * <code>AbstractEndpointSecurityFilter</code> provides basic initialisation for all security filters,
 * namely configuring the SecurityManager for this instance
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
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

    public final void initialise() throws InitialisationException
    {
        if (securityManager == null)
        {
            securityManager = MuleManager.getInstance().getSecurityManager();
        }
        if (securityManager == null)
        {
            throw new InitialisationException("No security Manager has been set");
        }
        if (endpoint == null)
        {
            throw new InitialisationException("The endpont is null for this Security filter");
        }
        //This filter may only allow authentication on a subset of registered security providers
        if (securityProviders != null)
        {
            UMOSecurityManager localManager = new MuleSecurityManager();
            String sp[] = Utility.split(securityProviders, ",");
            for (int i = 0; i < sp.length; i++)
            {
                UMOSecurityProvider provider = securityManager.getProvider(sp[i]);
                if (provider != null)
                {
                    localManager.addProvider(provider);
                } else
                {
                    throw new InitialisationException("There is not security provider registered called: " + sp[i]);
                }
            }
            securityManager = localManager;
        }
        if (endpoint.getType().equals(UMOEndpoint.ENDPOINT_TYPE_RECEIVER))
        {
            inbound = true;
        } else if (endpoint.getType().equals(UMOEndpoint.ENDPOINT_TYPE_SENDER))
        {
            inbound = false;
        } else
        {
            throw new InitialisationException("The Endpoint that this security filter is associated with must have a type of "
                    + UMOEndpoint.ENDPOINT_TYPE_SENDER + " or " + UMOEndpoint.ENDPOINT_TYPE_RECEIVER
                    + ". Endpoint type is set to " + endpoint.getType());
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

    public void authenticate(UMOEvent event) throws UMOSecurityException
    {
        if (inbound)
        {
            authenticateInbound(event);
        } else
        {
            authenticateOutbound(event);
        }
    }

    protected abstract void authenticateInbound(UMOEvent event) throws UMOSecurityException;

    protected abstract void authenticateOutbound(UMOEvent event) throws UMOSecurityException;

    protected abstract void doInitialise() throws InitialisationException;

}
