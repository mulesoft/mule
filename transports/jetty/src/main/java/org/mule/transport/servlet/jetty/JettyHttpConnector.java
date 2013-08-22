/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.ReplyToHandler;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.MuleContextNotification;
import org.mule.context.notification.NotificationException;
import org.mule.transport.AbstractConnector;
import org.mule.transport.servlet.JarResourceServlet;
import org.mule.transport.servlet.MuleReceiverServlet;
import org.mule.transport.servlet.MuleServletContextListener;
import org.mule.util.ClassUtils;
import org.mule.util.IOUtils;
import org.mule.util.StringMessageUtils;
import org.mule.util.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.annotations.Configuration;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.jetty.webapp.WebInfConfiguration;
import org.mortbay.log.Log;
import org.mortbay.xml.XmlConfiguration;

/**
 * The <code>JettyConnector</code> can be using to embed a Jetty server to receive requests on an
 * http inound endpoint. One server is created for each connector declared, many Jetty endpoints
 * can share the same connector.
 */
public class JettyHttpConnector extends AbstractConnector
{
    public static final String ROOT = "/";

    public static final String JETTY = "jetty";

    private Server httpServer;

    private String configFile;

    private JettyReceiverServlet receiverServlet;

    private boolean useContinuations = false;

    private int acceptors = 1;

    private String resourceBase;

    private WebappsConfiguration webappsConfiguration;

    protected HashMap<String, ConnectorHolder> holders = new HashMap<String, ConnectorHolder>();

    private WebAppDeployer deployer;

    public JettyHttpConnector(MuleContext context)
    {
        super(context);
        setupJettyLogging();
        registerSupportedProtocol("http");
        registerSupportedProtocol(JETTY);
        setInitialStateStopped(true);
    }

    protected void setupJettyLogging()
    {
        if ((Log.getLog() instanceof JettyLogger) == false)
        {
            Log.setLog(new JettyLogger());
        }
    }

    @Override
    public String getProtocol()
    {
        return JETTY;
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        httpServer = new Server()
        {
            @Override
            public void addHandler(Handler handler)
            {
                final Connector c = getServer().getConnectors()[0];
                if (handler instanceof WebAppContext)
                {
                    final WebAppContext webapp = (WebAppContext) handler;
                    final String msg = String.format("Will deploy a web app at %s:/%s%s%s",
                                                     "http", c.getHost(),
                                                     c.getPort() == 80 ? StringUtils.EMPTY : ":" + c.getPort(),
                                                     webapp.getContextPath());

                    final File workDir = new File(muleContext.getConfiguration().getWorkingDirectory(),
                                                  "_exploded_wars" + webapp.getContextPath());
                    workDir.mkdirs();
                    webapp.setTempDirectory(workDir);
                    // TODO extract to a better constant
                    webapp.setAttribute("muleContext", muleContext);

                    if (logger.isInfoEnabled())
                    {
                        logger.info(StringMessageUtils.getBoilerPlate(msg, '*', 70));
                    }
                }
                super.addHandler(handler);
            }
        };

        if (webappsConfiguration != null)
        {
            deployer = new WebAppDeployer();
            String webAppDir = webappsConfiguration.getDirectory();
            if (StringUtils.isBlank(webAppDir))
            {
                // if none specified, resolve defaults dynamically
                final String appDir = muleContext.getRegistry().get(MuleProperties.APP_HOME_DIRECTORY_PROPERTY);
                webAppDir = appDir + "/webapps";
            }

            if (configFile == null)
            {
                // override only if user hasn't specified one (turn off file-mapped buffer for
                // static files to avoid resource locking, makes webapp resources editable on the fly)
                final URL muleDefaults = ClassUtils.getResource("org/mule/transport/jetty/webdefault.xml", getClass());
                deployer.setDefaultsDescriptor(muleDefaults.toExternalForm());
            }
            deployer.setWebAppDir(webAppDir);
            deployer.setExtract(true);
            deployer.setParentLoaderPriority(false);
            deployer.setServerClasses(webappsConfiguration.getServerClasses());
            deployer.setSystemClasses(webappsConfiguration.getSystemClasses());

            org.mortbay.jetty.AbstractConnector jettyConnector = createJettyConnector();
            jettyConnector.setHost(webappsConfiguration.getHost());
            jettyConnector.setPort(webappsConfiguration.getPort());
            deployer.setContexts(httpServer);
            String[] confClasses = new String[]
            {
                // configures webapp's classloader as a child of a Mule app classloader
                WebInfConfiguration.class.getName(),
                // just to get jetty going, we don't need java ee bindings. inherits annotation processing
                DummyJndiConfiguration.class.getName()
            };
            deployer.setConfigurationClasses(confClasses);

            httpServer.addConnector(jettyConnector);
            httpServer.addLifeCycle(deployer);
        }

        initialiseFromConfigFile();

        try
        {
            muleContext.registerListener(new MuleContextNotificationListener<MuleContextNotification>(){
                @Override
                public void onNotification(MuleContextNotification notification)
                {
                    if (notification.getAction() == MuleContextNotification.CONTEXT_STARTED)
                    {
                        //We delay starting until the context has been started since we need the MuleAjaxServlet to initialise first
                        setInitialStateStopped(false);
                        try
                        {
                            start();
                            // update the agent displaying webapp urls to the user
                            final JettyWebappServerAgent agent = (JettyWebappServerAgent) muleContext.getRegistry().lookupAgent(JettyWebappServerAgent.NAME);
                            if (agent != null)
                            {
                                agent.onJettyConnectorStarted(JettyHttpConnector.this);
                            }
                        }
                        catch (MuleException e)
                        {
                            throw new MuleRuntimeException(CoreMessages.failedToStart(getName()), e);
                        }
                    }
                }
            });
        }
        catch (NotificationException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    @SuppressWarnings("unchecked")
    protected void initialiseFromConfigFile() throws InitialisationException
    {
        if (configFile == null)
        {
            return;
        }
        try
        {
            InputStream is = IOUtils.getResourceAsStream(configFile, getClass());
            XmlConfiguration config = new XmlConfiguration(is);

            String appHome =
                muleContext.getRegistry().lookupObject(MuleProperties.APP_HOME_DIRECTORY_PROPERTY);
            if (appHome == null)
            {
                // Mule IDE sets app.home as part of the launch config it creates
                appHome = System.getProperty(MuleProperties.APP_HOME_DIRECTORY_PROPERTY);
            }

            if (appHome != null)
            {
                config.getProperties().put(MuleProperties.APP_HOME_DIRECTORY_PROPERTY, appHome);
            }

            config.configure(httpServer);
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }

    /**
     * Template method to dispose any resources associated with this receiver. There
     * is not need to dispose the connector as this is already done by the framework
     */
    @Override
    protected void doDispose()
    {
        holders.clear();
    }

    @Override
    protected void doStart() throws MuleException
    {
        try
        {
            httpServer.start();

            if (deployer != null)
            {
                deployer.start();
            }

            for (ConnectorHolder<?, ?> contextHolder : holders.values())
            {
                contextHolder.start();
            }
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
            httpServer.stop();

            if (deployer != null)
            {
                deployer.stop();
            }

            for (ConnectorHolder<?, ?> connectorRef : holders.values())
            {
                connectorRef.stop();
            }
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
    @Override
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
    @Override
    protected void doDisconnect() throws Exception
    {
        //do nothing
    }

     @Override
    protected MessageReceiver createReceiver(FlowConstruct flowConstruct, InboundEndpoint endpoint) throws Exception
    {
        MessageReceiver receiver = super.createReceiver(flowConstruct, endpoint);
        registerJettyEndpoint(receiver, endpoint);
        return receiver;
    }

    protected org.mortbay.jetty.AbstractConnector createJettyConnector()
    {
        final SelectChannelConnector cnn = new SelectChannelConnector();

        // get and set number of acceptor threads into the underlying connector
        cnn.setAcceptors(getAcceptors());

        return cnn;
    }

    public void unregisterListener(MessageReceiver receiver) throws MuleException
    {
        String connectorKey = getHolderKey(receiver.getEndpoint());

        synchronized (this)
        {
            ConnectorHolder connectorRef = holders.get(connectorKey);
            if (connectorRef != null)
            {
                if (!connectorRef.isReferenced())
                {
                    getHttpServer().removeConnector(connectorRef.getConnector());
                    holders.remove(connectorKey);
                    connectorRef.stop();
                }
            }
        }
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

    @Override
    public ReplyToHandler getReplyToHandler(ImmutableEndpoint endpoint)
    {
        if (isUseContinuations())
        {
            return new JettyContinuationsReplyToHandler(muleContext);
        }
        return super.getReplyToHandler(endpoint);
    }

    public boolean isUseContinuations()
    {
        return useContinuations;
    }

    public void setUseContinuations(boolean useContinuations)
    {
        this.useContinuations = useContinuations;
    }

    /**
     * Get the number of "acceptor" threads Jetty should use
     * @return  the number of threads
     */
    public int getAcceptors() {
        return acceptors;
    }

    /**
     * Set the number of "acceptor" threads Jetty should use
     *
     * @param acceptors the number of threads
     */
    public void setAcceptors(final int acceptors) {
        this.acceptors = acceptors;
    }

    ConnectorHolder<? extends MuleReceiverServlet, ? extends JettyHttpMessageReceiver> registerJettyEndpoint(MessageReceiver receiver, InboundEndpoint endpoint) throws MuleException
    {
        // Make sure that there is a connector for the requested endpoint.
        String connectorKey = getHolderKey(endpoint);

        ConnectorHolder holder;

        synchronized (this)
        {
            holder = holders.get(connectorKey);
            if (holder == null)
            {
                Connector connector = createJettyConnector();

                connector.setPort(endpoint.getEndpointURI().getPort());
                String host = endpoint.getEndpointURI().getHost();
                if ("localhost".equalsIgnoreCase(host))
                {
                    // We default to binding the port on all local network interfaces if localhost is set.  This
                    // is the same behaviour as all other socket connectors in Mule
                    host = "0.0.0.0";
                }
                connector.setHost(host);
                getHttpServer().addConnector(connector);

                holder = createContextHolder(connector, receiver.getEndpoint(), receiver);
                holders.put(connectorKey, holder);
                if(isStarted())
                {
                    holder.start();
                }
            }
            else
            {
                holder.addReceiver(receiver);
            }
        }
        return holder;
    }

    protected ConnectorHolder createContextHolder(Connector connector, InboundEndpoint endpoint, MessageReceiver receiver)
    {
        return new MuleReceiverConnectorHolder(connector, (JettyReceiverServlet) createServlet(connector, endpoint), (JettyHttpMessageReceiver)receiver);
    }

    protected Servlet createServlet(Connector connector, ImmutableEndpoint endpoint)
    {
        HttpServlet servlet;
        if (getReceiverServlet() == null)
        {
            if(isUseContinuations())
            {
                servlet = new JettyContinuationsReceiverServlet();
            }
            else
            {
                servlet = new JettyReceiverServlet();
            }
        }
        else
        {
            servlet = getReceiverServlet();
        }

        String path = endpoint.getEndpointURI().getPath();
        if(StringUtils.isBlank(path))
        {
            path = ROOT;
        }

        ContextHandlerCollection handlerCollection = new ContextHandlerCollection();
        Context context = new Context(handlerCollection, ROOT, Context.NO_SECURITY);
        context.setConnectorNames(new String[]{connector.getName()});
        context.addEventListener(new MuleServletContextListener(muleContext, getName()));

        if (resourceBase != null)
        {
            Context resourceContext = new Context(handlerCollection, path, Context.NO_SECURITY);
            resourceContext.setResourceBase(resourceBase);
        }

        context.addServlet(JarResourceServlet.class, JarResourceServlet.DEFAULT_PATH_SPEC);

        ServletHolder holder = new ServletHolder();
        holder.setServlet(servlet);
        context.addServlet(holder, "/*");
        getHttpServer().addHandler(handlerCollection);
        return servlet;
    }

    protected String getHolderKey(ImmutableEndpoint endpoint)
    {
        return endpoint.getProtocol() + ":" + endpoint.getEndpointURI().getHost() + ":" + endpoint.getEndpointURI().getPort();
    }

    public class MuleReceiverConnectorHolder extends AbstractConnectorHolder<JettyReceiverServlet, JettyHttpMessageReceiver>
    {
        List<MessageReceiver> messageReceivers = new ArrayList<MessageReceiver>();

        public MuleReceiverConnectorHolder(Connector connector, JettyReceiverServlet servlet, JettyHttpMessageReceiver receiver)
        {
            super(connector, servlet, receiver);
            addReceiver(receiver);
        }

        @Override
        public boolean isReferenced()
        {
            return messageReceivers.size() > 0;
        }

        @Override
        public void addReceiver(JettyHttpMessageReceiver receiver)
        {
            messageReceivers.add(receiver);
            if(started)
            {
                getServlet().addReceiver(receiver);
            }
        }

        @Override
        public void removeReceiver(JettyHttpMessageReceiver receiver)
        {
            messageReceivers.remove(receiver);
            getServlet().removeReceiver(receiver);
        }

        @Override
        public void start() throws MuleException
        {
            super.start();

            for (MessageReceiver receiver : messageReceivers)
            {
                servlet.addReceiver(receiver);
            }
        }

        @Override
        public void stop() throws MuleException
        {
            super.stop();

            for (MessageReceiver receiver : messageReceivers)
            {
                servlet.removeReceiver(receiver);
            }
        }
    }

    public String getResourceBase()
    {
        return resourceBase;
    }

    public void setResourceBase(String resourceBase)
    {
        this.resourceBase = resourceBase;
    }

    public WebappsConfiguration getWebappsConfiguration()
    {
        return webappsConfiguration;
    }

    public void setWebappsConfiguration(WebappsConfiguration webappsConfiguration)
    {
        this.webappsConfiguration = webappsConfiguration;
    }

    /**
     * A helper method to differentiate between jetty-based connectors which can host full wars and ones which can't.
     */
    public boolean canHostFullWars()
    {
        return true;
    }

    /**
     * A helper class to let jetty startup, we don't bind java ee objects like java:comp/UserTransaction.
     */
    public static class DummyJndiConfiguration extends Configuration
    {

        public DummyJndiConfiguration() throws ClassNotFoundException
        {
        }

        @Override
        public void bindUserTransaction() throws Exception
        {
            // no-op
        }

        @Override
        protected void lockCompEnv() throws Exception
        {
            // no-op
        }
    }
}
