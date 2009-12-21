/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet.jetty;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.CoreMessages;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.service.TransportFactory;
import org.mule.transport.servlet.ServletConnector;
import org.mule.util.StringUtils;

/**
 * <code>JettyHttpMessageReceiver</code> is a simple http server that can be used to
 * listen for http requests on a particular port
 */
public class JettyHttpMessageReceiver extends AbstractMessageReceiver
{
    public static final String JETTY_SERVLET_CONNECTOR_NAME = "_jettyConnector";

    public JettyHttpMessageReceiver(Connector connector, Service service, InboundEndpoint endpoint)
            throws CreateException
    {
        super(connector, service, endpoint);

        if ("rest".equals(endpoint.getEndpointURI().getScheme()))
        {
            // We need to have a Servlet Connector pointing to our servlet so the Servlets can
            // find the listeners for incoming requests
            ServletConnector scon = (ServletConnector) new TransportFactory(connector.getMuleContext()).getConnectorByProtocol("servlet");
            if (scon == null)
            {
                scon = new ServletConnector();
                scon.setName(JETTY_SERVLET_CONNECTOR_NAME);
                scon.setServletUrl(endpoint.getEndpointURI().getAddress());
                scon.setMuleContext(connector.getMuleContext());
                try
                {
                    connector.getMuleContext().getRegistry().registerConnector(scon);
                }
                catch (MuleException e)
                {
                    throw new CreateException(e, this);
                }
            }

            try
            {
                String path = endpoint.getEndpointURI().getPath();
                if (StringUtils.isEmpty(path))
                {
                    path = "/";
                }

                MuleContext muleContext = connector.getMuleContext();
                URIBuilder uriBuilder = new URIBuilder("servlet://" + path.substring(1), muleContext);
                EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uriBuilder);
                endpointBuilder.setTransformers(endpoint.getTransformers());
                InboundEndpoint ep = 
                    muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(endpointBuilder);
                scon.registerListener(service, ep);
            }
            catch (Exception e)
            {
                throw new CreateException(e, this);
            }
        }

    }

    @Override
    protected void doConnect() throws Exception
    {
        // do nothing
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        // do nothing
    }

    /**
     * Template method to dispose any resources associated with this receiver. There
     * is not need to dispose the connector as this is already done by the framework
     */
    @Override
    protected void doDispose()
    {
        //Do nothing
    }

    @Override
    protected void doStart() throws MuleException
    {
        try
        {
            ((JettyHttpConnector) connector).registerListener(this);
        }
        catch (Exception e)
        {
            throw new LifecycleException(CoreMessages.failedToStart("Jetty Http Receiver"), e, this);
        }
    }

    @Override
    protected void doStop() throws MuleException
    {
        try
        {
            ((JettyHttpConnector) connector).unregisterListener(this);
        }
        catch (Exception e)
        {
            throw new LifecycleException(CoreMessages.failedToStop("Jetty Http Receiver"), e, this);
        }
    }
}
