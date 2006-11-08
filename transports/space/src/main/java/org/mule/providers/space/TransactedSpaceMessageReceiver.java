/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.space;

import org.apache.commons.collections.MapUtils;
import org.mule.config.i18n.Message;
import org.mule.impl.MuleMessage;
import org.mule.providers.ConnectException;
import org.mule.providers.TransactedPollingMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.space.UMOSpace;
import org.mule.umo.space.UMOSpaceException;

import java.util.List;
import java.util.Properties;

/**
 * Registers a transacted message listener on a Space.
 */
public class TransactedSpaceMessageReceiver extends TransactedPollingMessageReceiver
{
    private UMOSpace space;
    private SpaceConnector connector;

    public TransactedSpaceMessageReceiver(UMOConnector connector,
                                          UMOComponent component,
                                          final UMOEndpoint endpoint) throws InitialisationException
    {
        super(connector, component, endpoint, new Long(0));
        this.connector = (SpaceConnector)connector;
        this.frequency = MapUtils.getLongValue(endpoint.getProperties(), "frequency", 100000L);
    }

    protected List getMessages() throws Exception
    {
        Object message = space.take(frequency);
        if (message == null)
        {
            return null;
        }

        // Process message
        if (logger.isDebugEnabled())
        {
            logger.debug("Message received it is of type: " + message.getClass().getName());
        }

        UMOMessageAdapter adapter = connector.getMessageAdapter(message);
        routeMessage(new MuleMessage(adapter), true);
        return null;
    }

    protected void processMessage(Object message) throws Exception
    {
        // This method is never called as the message is processed when received
    }

    public void doConnect() throws Exception
    {
        String destination = endpoint.getEndpointURI().getAddress();

        Properties props = new Properties();
        props.putAll(endpoint.getProperties());
        try
        {
            logger.info("Connecting to space: " + destination);
            space = connector.getSpace(endpoint);
        }
        catch (UMOSpaceException e)
        {
            throw new ConnectException(new Message("space", 1, destination), e, this);
        }
    }

    public void doDisconnect() throws Exception
    {
        // template method
    }

    public UMOSpace getSpace()
    {
        return space;
    }
}
