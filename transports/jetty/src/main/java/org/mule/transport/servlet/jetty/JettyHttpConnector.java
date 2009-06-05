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

import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.ReplyToHandler;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.AbstractConnector;
import org.mule.util.IOUtils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.xml.XmlConfiguration;

/**
 * The <code>JettyConnector</code> can be using to embed a Jetty server to receive requests on an http inound endpoint.
 * One server is created for each connector declared, many Jetty endpoints can share the same connector.
 */

public class JettyHttpConnector extends AbstractConnector
{

    public static final String JETTY = "jetty";
    public static final String REST = "rest";

    private Server httpServer;

    private Map serverPorts;

    private String configFile;

    private JettyReceiverServlet receiverServlet;

    private Class servletClass;

    private ServletHolder holder;

    private boolean useContinuations = false;

    public JettyHttpConnector()
    {
        super();
        registerSupportedProtocol("http");
        registerSupportedProtocol("jetty");
        registerSupportedProtocol(REST);
        serverPorts = new HashMap(4);
    }

    public String getProtocol()
    {
        return JETTY;
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        httpServer = new Server();

        if (getReceiverServlet() == null)
        {
            setServletClass((useContinuations ? JettyContinuationsReceiverServlet.class :
                    JettyReceiverServlet.class));
        }
        
        ServletHandler handler = new ServletHandler();
        holder = handler.addServletWithMapping(getServletClass(), "/*");
        
        httpServer.addHandler(handler);
        
        if (configFile != null)
        {
            try
            {
                InputStream is = IOUtils.getResourceAsStream(configFile, getClass());
                XmlConfiguration config = new XmlConfiguration(is);
                config.configure(httpServer);
            }
            catch (Exception e)
            {
                throw new InitialisationException(e, this);
            }
        }
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
        serverPorts.clear();
    }

    protected void doStart() throws MuleException
    {
        try
        {
            httpServer.start();
            receiverServlet = (JettyReceiverServlet) holder.getServlet();
            for (Iterator iter = receivers.values().iterator(); iter.hasNext();)
            {
                MessageReceiver receiver = (MessageReceiver) iter.next();
                receiverServlet.addReceiver(receiver);
            }

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
            for (Iterator iter = receivers.values().iterator(); iter.hasNext();)
            {
                MessageReceiver receiver = (MessageReceiver) iter.next();
                receiverServlet.removeReceiver(receiver);
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
     *
     * @throws Exception
     */
    protected void doConnect() throws Exception
    {
        //do nothing
    }

    /**
     * Template method where any connected resources used by the connector should be
     * disconnected
     *
     * @throws Exception
     */
    protected void doDisconnect() throws Exception
    {
        //do nothing
    }


    void registerListener(MessageReceiver receiver) throws Exception
    {
        EndpointURI uri = receiver.getEndpointURI();
        if (serverPorts.keySet().contains(new Integer(uri.getPort())))
        {
            logger.debug("Http server already listening on: " + uri.getPort());
            receiverServlet.addReceiver(receiver);
            return;
        }

        //TODO
//        ThreadingProfile tp = getReceiverThreadingProfile();
//        getHttpServer().addConnector(socketListener);
//
//         QueuedThreadPool threadPool = new QueuedThreadPool();
//          threadPool.setMaxThreads(tp.getMaxThreadsActive());
//        threadPool.setMinThreads(tp.getMaxThreadsIdle());
//        threadPool.setMaxIdleTimeMs((int)tp.getThreadTTL());
//        //TODO exhaust action
//          httpServer.setThreadPool(threadPool);

        org.mortbay.jetty.AbstractConnector cnn = createJettyConnector();

        cnn.setPort(uri.getPort());

        httpServer.addConnector(cnn);

        serverPorts.put(new Integer(uri.getPort()), null);
        receiverServlet.addReceiver(receiver);

        cnn.start();
    }

    protected org.mortbay.jetty.AbstractConnector createJettyConnector()
    {
        return new SelectChannelConnector();
    }

    public boolean unregisterListener(MessageReceiver receiver)
    {
        return receiverServlet.removeReceiver(receiver);
    }

    public Server getHttpServer()
    {
        return httpServer;
    }

    public String getConfigFile()
    {
        return configFile;
    }

    public void setConfigFile(String configFile)
    {
        this.configFile = configFile;
    }

    public JettyReceiverServlet getReceiverServlet()
    {
        return receiverServlet;
    }

    public void setReceiverServlet(JettyReceiverServlet receiverServlet)
    {
        this.receiverServlet = receiverServlet;
    }

    protected JettyReceiverServlet createReceiverServlet()
    {
        return new JettyReceiverServlet();
    }

    public Class getServletClass()
    {
        return servletClass;
    }

    public void setServletClass(Class servletClass)
    {
        this.servletClass = servletClass;
    }

    /**
     * Getter for property 'replyToHandler'.
     *
     * @return Value for property 'replyToHandler'.
     */
    @Override
    public ReplyToHandler getReplyToHandler()
    {
        if (isUseContinuations())
        {
            return new JettyContinuationsReplyToHandler(getDefaultResponseTransformers());
        }
        return super.getReplyToHandler();
    }

    public boolean isUseContinuations()
    {
        return useContinuations;
    }

    public void setUseContinuations(boolean useContinuations)
    {
        this.useContinuations = useContinuations;
    }
}
