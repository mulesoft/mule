/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms;

import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.CreateException;
import org.mule.umo.provider.UMOConnector;

import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * Registers a single Jms MessageListener for an endpoint
 */
public class SingleJmsMessageReceiver extends JmsMessageReceiver implements MessageListener
{

    public SingleJmsMessageReceiver(UMOConnector connector, UMOComponent component, UMOEndpoint endpoint)
            throws CreateException
    {
        super(connector, component, endpoint);
    }


    public void onMessage(Message message)
    {
        try
        {
            JmsWorker worker = new JmsWorker(message, this);
            worker.run();
        }
        catch (Exception e)
        {
            handleException(e);
        }
    }
}
