/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ajax;

import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.MessageDispatcher;
import org.mule.transport.AbstractMessageDispatcherFactory;
import org.mule.transport.ajax.container.AjaxServletConnector;
import org.mule.transport.ajax.embedded.AjaxConnector;

import org.mortbay.cometd.AbstractBayeux;

/**
 * Creates a {@link AjaxMessageDispatcher}
 */
public class AjaxMessageDispatcherFactory extends AbstractMessageDispatcherFactory
{

    public MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException
    {
        AjaxMessageDispatcher dispatcher = new AjaxMessageDispatcher(endpoint);

        if (endpoint.getConnector() instanceof AjaxConnector)
        {
            //We're running in embedded mode (i.e. using a Jetty servlet container created by the connector)
            //so we need to register the endpoint
            dispatcher.setBayeux(((AjaxConnector) endpoint.getConnector()).getBayeux());
        }
        else
        {
            //We're bound to an existing servlet container, just grab the Bayeux object from the connector, which  would have been
            //set by the {@link MuleAjaxServlet}
            AbstractBayeux b = ((AjaxServletConnector) endpoint.getConnector()).getBayeux();
            if (b == null)
            {
                long start = System.currentTimeMillis();
                //Not the correct use for response time out but if fine for this purpose
                long timeout = start + endpoint.getResponseTimeout();
                while (start < timeout)
                {
                    try
                    {
                        Thread.sleep(1000);
                        b = ((AjaxServletConnector) endpoint.getConnector()).getBayeux();
                        if (b != null)
                        {
                            break;
                        }
                    }
                    catch (InterruptedException e)
                    {
                        //ignore
                    }
                }
                throw new IllegalArgumentException("Bayeux is null: " + endpoint.getConnector() + ". Waited for " +
                                                   endpoint.getResponseTimeout() + " for object to become availble, this usually caused if the servlet container takes a long time to start up");
            }
            dispatcher.setBayeux(b);
        }


        return dispatcher;
    }
}
