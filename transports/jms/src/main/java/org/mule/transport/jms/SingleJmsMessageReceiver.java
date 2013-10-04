/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;

import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;

import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * Registers a single Jms MessageListener for an endpoint
 */
public class SingleJmsMessageReceiver extends JmsMessageReceiver implements MessageListener
{

    public SingleJmsMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint)
            throws CreateException
    {
        super(connector, flowConstruct, endpoint);
    }


    public void onMessage(Message message)
    {
        JmsWorker worker = new JmsWorker(message, this);
        worker.run();
    }
}
