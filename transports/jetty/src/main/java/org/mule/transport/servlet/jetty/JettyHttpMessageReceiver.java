/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.transport.AbstractMessageReceiver;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

/**
 * <code>JettyHttpMessageReceiver</code> is a simple http server that can be used to
 * listen for http requests on a particular port
 */
public class JettyHttpMessageReceiver extends AbstractMessageReceiver
{
    public static final String JETTY_SERVLET_CONNECTOR_NAME = "_jettyConnector";

    public JettyHttpMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint)
        throws CreateException
    {
        super(connector, flowConstruct, endpoint);
    }
    
    public void routeMessageAsync(final MuleMessage message, final ContinuationsReplyTo continuationsReplyTo)
    {
        try
        {
            getWorkManager().scheduleWork(new Work() {

                public void run()
                {
                    try
                    {
                        MuleMessage threadSafeMessage = new DefaultMuleMessage(message);
                        routeMessage(threadSafeMessage);
                    }
                    catch (MuleException e)
                    {
                        continuationsReplyTo.setAndResume(e);
                    }
                }

                public void release()
                {
                    // nothing to clean up
                }
            });
        }
        catch (WorkException e)
        {
            getEndpoint().getMuleContext().getExceptionListener().handleException(e);
        }
    }
}
