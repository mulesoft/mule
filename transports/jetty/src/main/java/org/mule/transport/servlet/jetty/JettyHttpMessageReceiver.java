/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
            getConnector().getMuleContext().getExceptionListener().handleException(e);
        }
    }
}
