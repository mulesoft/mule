/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ajax.embedded;

import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.service.Service;
import org.mule.api.transport.MessageDispatcherFactory;
import org.mule.api.transport.MessageReceiver;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.ajax.AjaxMessageReceiver;
import org.mule.transport.ajax.container.AjaxServletConnector;
import org.mule.transport.ajax.i18n.AjaxMessages;

import java.util.HashMap;

import org.mortbay.cometd.AbstractBayeux;
import org.mortbay.cometd.continuation.ContinuationCometdServlet;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

/**
 * Creates an 'embedded' Ajax server using Jetty and allows Mule to receiver and send events 
 * to browsers. The browser will need to use the <pre>mule.js</pre> class to publish and 
 * subscribe events.
 */
public class AjaxConnector extends AjaxServletConnector
{
    public static final String PROTOCOL = "ajax";

    private Server httpServer;

    private HashMap<String, BayeuxHolder> connectors = new HashMap<String, BayeuxHolder>();



    public AjaxConnector()
    {
        super();
        registerSupportedProtocol("ajax");
        setInitialStateStopped(false);
    }

    public String getProtocol()
    {
        return PROTOCOL;
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        httpServer = new Server();
//
//        if (configFile != null)
//        {
//            try
//            {
//                InputStream is = IOUtils.getResourceAsStream(configFile, getClass());
//                XmlConfiguration config = new XmlConfiguration(is);
//                config.configure(httpServer);
//            }
//            catch (Exception e)
//            {
//                throw new InitialisationException(e, this);
//            }
//        }
    }

    AbstractBayeux getBayeux(ImmutableEndpoint endpoint)
    {
         String connectorKey = endpoint.getProtocol() + ":" + endpoint.getEndpointURI().getHost() + ":" + endpoint.getEndpointURI().getPort();

        synchronized (connectors)
        {
            BayeuxHolder connectorRef = connectors.get(connectorKey);
            if(connectorRef!=null)
            {
                return connectorRef.servlet.getBayeux();
            }
        }
        throw new IllegalArgumentException("Endpoiont not registered: " + connectorKey);
    }

    /**
     * Template method to dispose any resources associated with this receiver. There
     * is not need to dispose the connector as this is already done by the framework
     */
    protected void doDispose()
    {
        try
        {
            httpServer.stop();
        }
        catch (Exception e)
        {
            logger.error("Error disposing Jetty server", e);
        }
        connectors.clear();
    }

    protected void doStart() throws MuleException
    {
        try
        {
            httpServer.start();

        }
        catch (Exception e)
        {
            throw new LifecycleException(CoreMessages.failedToStart("Jetty Http Receiver"), e, this);
        }
    }

    protected void doStop() throws MuleException
    {
        try
        {
            for (BayeuxHolder connectorRef : connectors.values())
            {
                connectorRef.connector.stop();
            }

            httpServer.stop();

        }
        catch (Exception e)
        {
            throw new LifecycleException(CoreMessages.failedToStop("Jetty Http Receiver"), e, this);
        }
    }


    /**
     * Template method where any connections should be made for the connector
     */
    protected void doConnect() throws Exception
    {
        //do nothing
    }

    /**
     * Template method where any connected resources used by the connector should be
     * disconnected
     */
    protected void doDisconnect() throws Exception
    {
        //do nothing
    }

    @Override
    public MessageReceiver registerListener(Service service, InboundEndpoint endpoint) throws Exception
    {
        MessageReceiver receiver = super.registerListener(service, endpoint);
        BayeuxHolder holder = registerBayeuxEndpoint(receiver.getEndpoint());
        ((AjaxMessageReceiver) receiver).setBayeux(holder.getBayeux());

        return receiver;
    }

    public BayeuxHolder registerBayeuxEndpoint(ImmutableEndpoint endpoint) throws MuleException
    {
        // Make sure that there is a connector for the requested endpoint.
        String connectorKey = endpoint.getProtocol() + ":" + endpoint.getEndpointURI().getHost() + ":" + endpoint.getEndpointURI().getPort();

        BayeuxHolder holder;

        synchronized (connectors)
        {
            holder = connectors.get(connectorKey);
            if (holder == null)
            {
                Connector connector = createJettyConnector();

                connector.setPort(endpoint.getEndpointURI().getPort());
                connector.setHost(endpoint.getEndpointURI().getHost());
                if ("localhost".equalsIgnoreCase(endpoint.getEndpointURI().getHost()))
                {
                    logger.warn("You use localhost interface! It means that no external connections will be available."
                            + " Don't you want to use 0.0.0.0 instead (all network interfaces)?");
                }
                getHttpServer().addConnector(connector);

                ContinuationCometdServlet servlet = createServletForConnector(connector, endpoint);
                holder = new BayeuxHolder(connector, servlet);
                if(getBayeux()==null) setBayeux(servlet.getBayeux());
               // connector.start();

                connectors.put(connectorKey, holder);
            }
            else
            {
                holder.increment();
            }
            AbstractBayeux bayeux = holder.servlet.getBayeux();
            bayeux.setJSONCommented(isJsonCommented());
        }
        return holder;
    }


    @Override
    public void destroyReceiver(MessageReceiver receiver, ImmutableEndpoint endpoint) throws Exception
    {
        unregisterConnectorListener(receiver);
        super.destroyReceiver(receiver, endpoint);
    }

    void unregisterConnectorListener(MessageReceiver receiver) throws Exception
    {
        InboundEndpoint endpoint = receiver.getEndpoint();

        String connectorKey = endpoint.getProtocol() + ":" + endpoint.getEndpointURI().getHost() + ":" + endpoint.getEndpointURI().getPort();

        synchronized (connectors)
        {
            BayeuxHolder connectorRef = connectors.get(connectorKey);
            if (connectorRef != null)
            {
                if (connectorRef.decrement() == 0)
                {
                    getHttpServer().removeConnector(connectorRef.connector);
                    connectorRef.connector.stop();
                    connectors.remove(connectorKey);
                }
            }
        }

    }

    @Override
    public void setDispatcherFactory(MessageDispatcherFactory dispatcherFactory)
    {
        super.setDispatcherFactory(dispatcherFactory);    //To change body of overridden methods use File | Settings | File Templates.
    }

    protected org.mortbay.jetty.AbstractConnector createJettyConnector()
    {
        return new SelectChannelConnector();
    }

    public Server getHttpServer()
    {
        return httpServer;
    }

    protected ContinuationCometdServlet createServletForConnector(Connector connector, ImmutableEndpoint endpoint) throws MuleException
    {

        ContinuationCometdServlet servlet = new ContinuationCometdServlet();

        Context context = new Context(this.getHttpServer(), "/", Context.NO_SESSIONS);
        context.setConnectorNames(new String[]{connector.getName()});

        ServletHolder holder = new ServletHolder();
        holder.setServlet(servlet);
        context.setResourceBase(endpoint.getEndpointURI().getPath());
        context.addServlet(holder, "/ajax/*");
        context.addServlet("org.mortbay.jetty.servlet.DefaultServlet", "/");

        try
        {
            connector.start();
            context.start();
        }
        catch (Exception e)
        {
            throw new InitialisationException(AjaxMessages.failedToStartAjaxServlet(), e, this);
        }

        if(getInterval() != INT_VALUE_NOT_SET) holder.setInitParameter("interval", Integer.toString(getInterval()));
        holder.setInitParameter("JSONCommented", Boolean.toString(isJsonCommented()));
        if(getLogLevel() != INT_VALUE_NOT_SET) holder.setInitParameter("logLevel", Integer.toString(getLogLevel()));
        if(getMaxInterval() != INT_VALUE_NOT_SET) holder.setInitParameter("maxInterval", Integer.toString(getMaxInterval()));
        if(getMultiFrameInterval() != INT_VALUE_NOT_SET) holder.setInitParameter("multiFrameInterval", (Integer.toString(getMultiFrameInterval())));
        if(getTimeout() != INT_VALUE_NOT_SET) holder.setInitParameter("timeout", Integer.toString(getTimeout()));
        if(getRefsThreshold() != INT_VALUE_NOT_SET) holder.setInitParameter("refsThreshold", Integer.toString(getRefsThreshold()));
        holder.setInitParameter("requestAvailable", Boolean.toString(isRequestAvailable()));
        holder.setInitParameter("directDeliver", Boolean.toString(isDirectDeliver()));

        return servlet;
    }

    public class BayeuxHolder
    {
        Connector connector;
        ContinuationCometdServlet servlet;
        int refCount;

        public BayeuxHolder(Connector connector,
                            ContinuationCometdServlet servlet)
        {
            this.connector = connector;
            this.servlet = servlet;
            increment();
        }

        public int increment()
        {
            return ++refCount;
        }

        public int decrement()
        {
            return --refCount;
        }

        public AbstractBayeux getBayeux()
        {
            return servlet.getBayeux();
        }
    }

}
