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
package org.mule.providers.soap.glue;

import electric.server.http.HTTP;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.providers.AbstractServiceEnabledConnector;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.LifecycleException;
import org.mule.umo.provider.UMOMessageReceiver;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <code>GlueConnector</code> instanciates a Glue soap server and allows beans to be
 * dynamically exposed a swebservices simply by registering with the connector
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class GlueConnector extends AbstractServiceEnabledConnector
{
    private List serverEndpoints = new ArrayList();
    private Map context;

    public String getProtocol()
    {
        return "glue";
    }

    /**
     * Template method to perform any work when stopping the connectoe
     *
     * @throws org.mule.umo.UMOException if the method fails
     */
    protected void stopConnector() throws UMOException
    {
        String endpoint = null;
        try
        {
            for (Iterator iterator = serverEndpoints.iterator(); iterator.hasNext();)
            {
                endpoint = (String) iterator.next();
                HTTP.shutdown(endpoint);
            }
            serverEndpoints.clear();
        } catch (IOException e)
        {
            throw new LifecycleException(new Message("soap", 1, endpoint), e, this);
        }
    }

    public UMOMessageReceiver registerListener(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        if (endpoint == null || component == null)
            throw new IllegalArgumentException("The endpoint and component cannot be null when registering a listener");

        if (endpoint.getEndpointURI() == null)
        {
            throw new InitialisationException(new Message(Messages.ENDPOINT_NULL_FOR_LISTENER), this);
        }
        UMOMessageReceiver receiver = null;

        logger.info("registering listener: " + component.getDescriptor().getName() + " on endpointUri: " + endpoint.getEndpointURI().getAddress());
        if (shouldCreateServer(endpoint.getEndpointURI().getAddress()))
        {
            receiver = new GlueMessageReceiver(this, component, endpoint, true);
            receivers.put(endpoint.getEndpointURI().getAddress(), receiver);
            serverEndpoints.add(endpoint.getEndpointURI().getAddress());
        } else
        {
            receiver = new GlueMessageReceiver(this, component, endpoint, false);
            receivers.put(endpoint.getEndpointURI().getAddress() + "/" + component.getDescriptor().getName(), receiver);
        }
        return receiver;
    }

    private boolean shouldCreateServer(String endpoint) throws URISyntaxException
    {
        URI uri = new URI(endpoint);
        String ep = uri.getScheme() + "://" + uri.getHost();
        if(uri.getPort()!= -1) ep += ":" + uri.getPort();

        for (Iterator iterator = serverEndpoints.iterator(); iterator.hasNext();)
        {
            String s = (String) iterator.next();
            if(s.startsWith(ep)) {
                return false;
            }
        }
        return true;
    }

    public Map getContext()
    {
        return context;
    }

    public void setContext(Map context)
    {
        this.context = context;
    }
}
