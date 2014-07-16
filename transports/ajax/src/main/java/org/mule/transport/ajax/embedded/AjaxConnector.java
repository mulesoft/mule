/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ajax.embedded;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.ReplyToHandler;
import org.mule.transport.ajax.AjaxMessageReceiver;
import org.mule.transport.ajax.AjaxMuleMessageFactory;
import org.mule.transport.ajax.AjaxReplyToHandler;
import org.mule.transport.ajax.BayeuxAware;
import org.mule.transport.ajax.container.MuleAjaxServlet;
import org.mule.transport.ajax.i18n.AjaxMessages;
import org.mule.transport.servlet.JarResourceServlet;
import org.mule.transport.servlet.MuleServletContextListener;
import org.mule.transport.servlet.jetty.JettyHttpsConnector;
import org.mule.util.StringUtils;

import java.net.URL;
import java.util.Map;

import javax.servlet.Servlet;

import org.eclipse.jetty.server.AbstractNetworkConnector;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.mortbay.cometd.AbstractBayeux;
import org.mortbay.cometd.continuation.ContinuationCometdServlet;

/**
 * Creates an 'embedded' Ajax server using Jetty and allows Mule to receiver and send
 * events to browsers. The browser will need to use the
 *
 * <pre>
 * mule.js
 * </pre>
 *
 * class to publish and subscribe events. Note that a resource base property can be
 * set on the ajax endpoint that provides the location of any web application
 * resources such as html pages.
 */
public class AjaxConnector extends JettyHttpsConnector implements BayeuxAware
{
    public static final String PROTOCOL = "ajax";

    public static final String CHANNEL_PROPERTY = "channel";

    public static final String AJAX_PATH_SPEC = "/ajax/*";

    public static final String COMETD_CLIENT = "cometd.client";

    /**
     * This is the key that's used to retrieve the reply to destination from a {@link Map} that's
     * passed into {@link AjaxMuleMessageFactory}.
     */
    public static final String REPLYTO_PARAM = "replyTo";

    private URL serverUrl;

    /**
     * The client side poll timeout in milliseconds (default 0). How long a client
     * will wait between reconnects
     */
    private int interval = INT_VALUE_NOT_SET;

    /**
     * The max client side poll timeout in milliseconds (default 30000). A client
     * will be removed if a connection is not received in this time.
     */
    private int maxInterval = INT_VALUE_NOT_SET;

    /**
     * The client side poll timeout if multiple connections are detected from the
     * same browser (default 1500).
     */
    private int multiFrameInterval = INT_VALUE_NOT_SET;

    /**
     * 0=none, 1=info, 2=debug
     */
    private int logLevel = INT_VALUE_NOT_SET;

    /**
     * The server side poll timeout in milliseconds (default 250000). This is how long
     * the server will hold a reconnect request before responding.
     */
    private int timeout = INT_VALUE_NOT_SET;

    /**
     * If "true" (default) then the server will accept JSON wrapped in a comment and
     * will generate JSON wrapped in a comment. This is a defence against Ajax Hijacking.
     */
    private boolean jsonCommented = true;

    /**
     * TODO SUPPORT FILTERS
     * the location of a JSON file describing {@link org.cometd.DataFilter} instances to be installed
     */
    private String filters;

    /**
     * If true, the current request is made available via the
     * {@link AbstractBayeux#getCurrentRequest()} method
     */
    private boolean requestAvailable = true;

    /**
     * true if published messages are delivered directly to subscribers (default).
     * If false, a message copy is created with only supported fields (default true).
     */
    private boolean directDeliver = true;

    /**
     * The number of message refs at which the a single message response will be
     * cached instead of being generated for every client delivered to. Done to optimize
     * a single message being sent to multiple clients.
     */
    private int refsThreshold = INT_VALUE_NOT_SET;

    /**
     * By default, an asynchronous reply to the inbound endpoint is sent back.  This can cause unwanted side effects
     * in some cases, use this attribute to disable.
     */
    private boolean disableReplyTo = false;

    private ContinuationCometdServlet servlet;

    public AjaxConnector(MuleContext context)
    {
        super(context);
        unregisterSupportedProtocol("http");
        unregisterSupportedProtocol("https");
        unregisterSupportedProtocol("jetty-ssl");
        unregisterSupportedProtocol("jetty");
        setInitialStateStopped(true);
    }

    @Override
    public String getProtocol()
    {
        return PROTOCOL;
    }

    public URL getServerUrl()
    {
        return serverUrl;
    }

    public void setServerUrl(URL serverUrl)
    {
        this.serverUrl = serverUrl;
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        if (serverUrl==null)
        {
            throw new InitialisationException(AjaxMessages.serverUrlNotDefined(), this);
        }
        super.doInitialise();
        try
        {
            createEmbeddedServer();
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }

    @Override
    protected void doStart() throws MuleException
    {
        super.doStart();
        for (MessageReceiver receiver : receivers.values())
        {
            ((AjaxMessageReceiver)receiver).setBayeux(getBayeux());
        }
    }

    @Override
    protected void validateSslConfig() throws InitialisationException
    {
        if (serverUrl.getProtocol().equals("https"))
        {
            super.validateSslConfig();
        }
    }

    @Override
    public ReplyToHandler getReplyToHandler(ImmutableEndpoint endpoint)
    {
        return new AjaxReplyToHandler(this, endpoint.getMuleContext());
    }

    void createEmbeddedServer() throws MuleException
    {
        AbstractNetworkConnector connector = createJettyConnector();

        configureConnector(connector, serverUrl.getHost(), serverUrl.getPort());

        getHttpServer().addConnector(connector);
        EndpointBuilder builder = muleContext.getEndpointFactory().getEndpointBuilder(serverUrl.toString());

        servlet = (ContinuationCometdServlet)createServlet(connector, builder.buildInboundEndpoint());
    }

    @Override
    public Servlet createServlet(AbstractNetworkConnector connector, ImmutableEndpoint endpoint)
    {
        ContinuationCometdServlet ajaxServlet = new MuleAjaxServlet();

        String path = endpoint.getEndpointURI().getPath();
        if (StringUtils.isBlank(path))
        {
            path = ROOT;
        }

        ContextHandlerCollection handlerCollection = new ContextHandlerCollection();
        ServletContextHandler root = new ServletContextHandler(handlerCollection, ROOT, ServletContextHandler.NO_SECURITY);
        root.setVirtualHosts(new String[] { getVirtualHostName(connector) });
        root.addEventListener(new MuleServletContextListener(muleContext, getName()));

        if (!ROOT.equals(path))
        {
            ServletContextHandler resourceContext = new ServletContextHandler(handlerCollection, path, ServletContextHandler.NO_SECURITY);
            populateContext(resourceContext);

        }
        else
        {
            populateContext(root);
        }

        //Add ajax to root
        ServletHolder holder = new ServletHolder();
        holder.setServlet(ajaxServlet);
        root.addServlet(holder, AJAX_PATH_SPEC);

        if (getInterval() != INT_VALUE_NOT_SET) holder.setInitParameter("interval", Integer.toString(getInterval()));
        holder.setInitParameter("JSONCommented", Boolean.toString(isJsonCommented()));
        if (getLogLevel() != INT_VALUE_NOT_SET) holder.setInitParameter("logLevel", Integer.toString(getLogLevel()));
        if (getMaxInterval() != INT_VALUE_NOT_SET) holder.setInitParameter("maxInterval", Integer.toString(getMaxInterval()));
        if (getMultiFrameInterval() != INT_VALUE_NOT_SET) holder.setInitParameter("multiFrameInterval", (Integer.toString(getMultiFrameInterval())));
        if (getTimeout() != INT_VALUE_NOT_SET) holder.setInitParameter("timeout", Integer.toString(getTimeout()));
        if (getRefsThreshold() != INT_VALUE_NOT_SET) holder.setInitParameter("refsThreshold", Integer.toString(getRefsThreshold()));
        holder.setInitParameter("requestAvailable", Boolean.toString(isRequestAvailable()));


        this.addHandler(handlerCollection);
        return ajaxServlet;
    }

    protected void populateContext(ServletContextHandler context)
    {
        context.addServlet(DefaultServlet.class, ROOT);
        context.addServlet(JarResourceServlet.class, JarResourceServlet.DEFAULT_PATH_SPEC);
        context.addEventListener(new MuleServletContextListener(muleContext, getName()));
        String base = getResourceBase();
        if (base != null)
        {
            context.setResourceBase(base);
        }
    }

    @Override
    protected AbstractNetworkConnector createJettyConnector()
    {
        if (serverUrl.getProtocol().equals("https"))
        {
            return super.createJettyConnector();
        }
        else
        {
            ServerConnector serverConnector = new ServerConnector(getHttpServer());
            serverConnector.setName(getName());
            return serverConnector;
        }
    }

    @Override
    public AbstractBayeux getBayeux( )
    {
        return servlet.getBayeux();
    }

    @Override
    public void setBayeux(AbstractBayeux bayeux)
    {
        //Ignore
    }

    @Override
    protected MessageReceiver createReceiver(FlowConstruct flowConstruct, InboundEndpoint endpoint) throws Exception
    {
        MessageReceiver receiver = getServiceDescriptor().createMessageReceiver(this, flowConstruct, endpoint);
        //If the connector has not started yet, the Bayeux object will still be null
        ((AjaxMessageReceiver) receiver).setBayeux(getBayeux());
        return receiver;
    }


    public int getInterval()
    {
        return interval;
    }

    public void setInterval(int interval)
    {
        this.interval = interval;
    }

    public int getMaxInterval()
    {
        return maxInterval;
    }

    public void setMaxInterval(int maxInterval)
    {
        this.maxInterval = maxInterval;
    }

    public int getMultiFrameInterval()
    {
        return multiFrameInterval;
    }

    public void setMultiFrameInterval(int multiFrameInterval)
    {
        this.multiFrameInterval = multiFrameInterval;
    }

    public int getLogLevel()
    {
        return logLevel;
    }

    public void setLogLevel(int logLevel)
    {
        this.logLevel = logLevel;
    }

    public int getTimeout()
    {
        return timeout;
    }

    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }

    public boolean isJsonCommented()
    {
        return jsonCommented;
    }

    public void setJsonCommented(boolean jsonCommented)
    {
        this.jsonCommented = jsonCommented;
    }

    public String getFilters()
    {
        return filters;
    }

    public void setFilters(String filters)
    {
        this.filters = filters;
    }

    public boolean isRequestAvailable()
    {
        return requestAvailable;
    }

    public void setRequestAvailable(boolean requestAvailable)
    {
        this.requestAvailable = requestAvailable;
    }

    public boolean isDirectDeliver()
    {
        return directDeliver;
    }

    public void setDirectDeliver(boolean directDeliver)
    {
        this.directDeliver = directDeliver;
    }

    public int getRefsThreshold()
    {
        return refsThreshold;
    }

    public void setRefsThreshold(int refsThreshold)
    {
        this.refsThreshold = refsThreshold;
    }

    @Override
    public boolean canHostFullWars()
    {
        // ajax connector doesn't host full wars, flag this to Mule
        return false;
    }

    public void setDisableReplyTo(boolean disableReplyTo)
    {
        this.disableReplyTo = disableReplyTo;
    }

    public boolean isDisableReplyTo()
    {
        return disableReplyTo;
    }
}
