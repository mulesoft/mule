/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.client;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.VoidMuleEvent;
import org.mule.api.FutureMessageResult;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.ConfigurationException;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.ServiceException;
import org.mule.api.service.Service;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.ReceiveException;
import org.mule.client.DefaultLocalMuleClient;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.security.MuleCredentials;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.transformer.TransformerUtils;
import org.mule.transport.NullPayload;
import org.mule.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MuleClient</code> is a simple interface for Mule clients to send and
 * receive events from a Mule Server. In most Mule applications events are triggered
 * by some external occurrence such as a message being received on a queue or a file
 * being copied to a directory. The Mule client allows the user to send and receive
 * events programmatically through its API.
 * <p>
 * The client defines a {@link EndpointURI} which is used to determine how a message is
 * sent of received. The url defines the protocol, the endpointUri destination of the
 * message and optionally the endpoint to use when dispatching the event. For
 * example:
 * <p>
 * <code>vm://my.object</code> dispatches to a <code>my.object</code> destination
 * using the VM endpoint. There needs to be a global VM endpoint registered for the
 * message to be sent.
 * <p>
 * <code>jms://jmsProvider/orders.topic</code> dispatches a JMS message via the
 * globally registered jmsProvider over a topic destination called
 * <code>orders.topic</code>.
 * <p>
 * <code>jms://orders.topic</code> is equivalent to the above except that the
 * endpoint is determined by the protocol, so the first JMS endpoint is used.
 * <p>
 * Note that there must be a configured MuleManager for this client to work. It will
 * use the one available using <code>muleContext</code>
 *
 * @see org.mule.endpoint.MuleEndpointURI
 */
public class MuleClient implements Disposable
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(MuleClient.class);

    /**
     * The local MuleContext instance.
     */
    private MuleContext muleContext;

    private List<RemoteDispatcher> dispatchers = new ArrayList<RemoteDispatcher>();

    private MuleCredentials user;

    private DefaultMuleContextFactory muleContextFactory = new DefaultMuleContextFactory();

    private ConcurrentMap<String, InboundEndpoint> inboundEndpointCache = new ConcurrentHashMap<String, InboundEndpoint>();
    private ConcurrentMap<String, OutboundEndpoint> outboundEndpointCache = new ConcurrentHashMap<String, OutboundEndpoint>();

    /**
     * Creates a Mule client that will use the default serverEndpoint when connecting to a remote
     * server instance.
     *
     * @throws MuleException
     */
    protected MuleClient() throws MuleException
    {
        this(true);
    }

    public MuleClient(boolean startContext) throws MuleException
    {
        init(startContext);
    }

    public MuleClient(MuleContext context) throws MuleException
    {
        this.muleContext = context;
        init(false);
    }

    /**
     * Configures a Mule client instance using the the default
     * {@link SpringXmlConfigurationBuilder} to parse <code>configResources</code>.
     *
     * @param configResources a config resource location to configure this client
     *            with
     * @throws ConfigurationException if there is a {@link MuleContext} instance already
     *             running in this JVM or if the builder fails to configure the
     *             Manager
     */
    public MuleClient(String configResources) throws MuleException
    {
        this(configResources, new SpringXmlConfigurationBuilder(configResources));
    }

    /**
     * Configures a new Mule client and either uses an existing Manager running in
     * this JVM or creates a new empty {@link MuleContext}
     *
     * @param user the username to use when connecting to a remote server instance
     * @param password the password for the user
     * @throws MuleException
     */
    public MuleClient(String user, String password) throws MuleException
    {
        init(/* startManager */true);
        this.user = new MuleCredentials(user, password.toCharArray());
    }

    /**
     * Configures a Mule client instance
     *
     * @param configResources a config resource location to configure this client
     *            with
     * @param builder the configuration builder to use
     * @throws ConfigurationException is there is a {@link MuleContext} instance already
     *             running in this JVM or if the builder fails to configure the
     *             Manager
     * @throws InitialisationException
     */
    public MuleClient(String configResources, ConfigurationBuilder builder)
        throws ConfigurationException, InitialisationException
    {
        if (builder == null)
        {
            logger.info("Builder passed in was null, using default builder: "
                        + SpringXmlConfigurationBuilder.class.getName());
            builder = new SpringXmlConfigurationBuilder(configResources);
        }
        logger.info("Initializing Mule...");
        muleContext = muleContextFactory.createMuleContext(builder);
    }

    /**
     * Configures a Mule client instance
     *
     * @param configResources a config resource location to configure this client
     *            with
     * @param builder the configuration builder to use
     * @param user the username to use when connecting to a remote server instance
     * @param password the password for the user
     * @throws ConfigurationException is there is a {@link MuleContext} instance already
     *             running in this JVM or if the builder fails to configure the
     *             Manager
     * @throws InitialisationException
     */
    public MuleClient(String configResources, ConfigurationBuilder builder, String user, String password)
        throws ConfigurationException, InitialisationException
    {
        this(configResources, builder);
        this.user = new MuleCredentials(user, password.toCharArray());
    }

    /**
     * Initialises a default {@link MuleContext} for use by the client.
     *
     * @param startManager start the Mule context if it has not yet been initialised
     * @throws MuleException
     */
    private void init(boolean startManager) throws MuleException
    {
        if (muleContext == null)
        {
            logger.info("No existing ManagementContext found, creating a new Mule instance");

            MuleContextBuilder contextBuilder = new DefaultMuleContextBuilder();
            DefaultMuleConfiguration config = new DefaultMuleConfiguration();
            config.setClientMode(true);
            contextBuilder.setMuleConfiguration(config);
            muleContext = muleContextFactory.createMuleContext(contextBuilder);
        }
        else
        {
            logger.info("Using existing MuleContext: " + muleContext);
        }

        if (!muleContext.isStarted() && startManager == true)
        {
            logger.info("Starting Mule...");
            muleContext.start();
        }
    }

    /**
     * Dispatches an event asynchronously to a endpointUri via a Mule server. The URL
     * determines where to dispatch the event to.
     *
     * @param url the Mule URL used to determine the destination and transport of the
     *            message
     * @param payload the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload. In
     *            the case of JMS you could set the JMSReplyTo property in these
     *            properties.
     * @throws org.mule.api.MuleException
     */
    public void dispatch(String url, Object payload, Map<String, Object> messageProperties) throws MuleException
    {
        dispatch(url, new DefaultMuleMessage(payload, messageProperties, muleContext));
    }

    /**
     * Dispatches an event asynchronously to a endpointUri via a Mule server. The URL
     * determines where to dispatch the event to.
     *
     * @param url the Mule URL used to determine the destination and transport of the
     *            message
     * @param message the message to send
     * @throws org.mule.api.MuleException
     */
    public void dispatch(String url, MuleMessage message) throws MuleException
    {
        OutboundEndpoint endpoint = getOutboundEndpoint(url, MessageExchangePattern.ONE_WAY, null);
        MuleEvent event = getEvent(message, MessageExchangePattern.ONE_WAY);
        endpoint.process(event);
    }

    /**
     * Sends an event synchronously to a component
     *
     * @param component the name of the Mule component to send to
     * @param transformers a comma separated list of transformers to apply to the
     *            result message
     * @param payload the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload. as
     *            null
     * @return the result message if any of the invocation
     * @throws org.mule.api.MuleException if the dispatch fails or the components or
     *             transfromers cannot be found
     *
     * <b>Deprecated from 3.6.0</b> - Only supported with Services. Sending message to remote flows is not supported.
     */
    @Deprecated
    public MuleMessage sendDirect(String component, String transformers, Object payload, Map<String, Object> messageProperties)
        throws MuleException
    {
        MuleMessage message = new DefaultMuleMessage(payload, messageProperties, muleContext);
        return sendDirect(component, transformers, message);
    }

    /**
     * Sends an event synchronously to a component
     *
     * @param componentName the name of the Mule component to send to
     * @param transformers a comma separated list of transformers to apply to the
     *            result message
     * @param message the message to send
     * @return the result message if any of the invocation
     * @throws org.mule.api.MuleException if the dispatch fails or the components or
     *             transfromers cannot be found
     *
     * <b>Deprecated from 3.6.0</b> - Only supported with Services. Sending message to remote flows is not supported.
     */
    @Deprecated
    public MuleMessage sendDirect(String componentName, String transformers, MuleMessage message)
        throws MuleException
    {
        Service service = muleContext.getRegistry().lookupService(componentName);
        if (service == null)
        {
            throw new ServiceException(CoreMessages.objectNotRegistered("Service", componentName));
        }
        List<Transformer> trans = null;
        if (transformers != null)
        {
            trans = TransformerUtils.getTransformers(transformers, muleContext);
        }

        InboundEndpoint endpoint = getDefaultClientEndpoint(service, message.getPayload(), true);
        MuleEvent event = new DefaultMuleEvent(message, endpoint, service);

        if (logger.isDebugEnabled())
        {
            logger.debug("MuleClient sending event direct to: " + componentName + ". MuleEvent is: " + event);
        }

        MuleEvent resultEvent = service.sendEvent(event);
        MuleMessage result = resultEvent == null || VoidMuleEvent.getInstance().equals(resultEvent)
                                                                                                   ? null
                                                                                                   : resultEvent.getMessage();
        if (logger.isDebugEnabled())
        {
            logger.debug("Result of MuleClient sendDirect is: "
                         + (result == null ? "null" : result.getPayload()));
        }

        if (result != null && trans != null)
        {
            result.applyTransformers(resultEvent, trans);
        }
        return result;
    }

    /**
     * Dispatches an event asynchronously to a component
     *
     * @param component the name of the Mule components to dispatch to
     * @param payload the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload. as
     *            null
     * @throws org.mule.api.MuleException if the dispatch fails or the components or
     *             transfromers cannot be found
     *
     * <b>Deprecated from 3.6.0</b> - Only supported with Services. Sending message to remote flows is not supported.
     */
    @Deprecated
    public void dispatchDirect(String component, Object payload, Map<String, Object> messageProperties) throws MuleException
    {
        dispatchDirect(component, new DefaultMuleMessage(payload, messageProperties, muleContext));
    }

    /**
     * Dispatches an event asynchronously to a component
     *
     * @param componentName the name of the Mule components to dispatch to
     * @param message the message to send
     * @throws org.mule.api.MuleException if the dispatch fails or the components or
     *             transfromers cannot be found
     *
     * <b>Deprecated from 3.6.0</b> - Only supported with Services. Sending message to remote flows is not supported.
     */
    @Deprecated
    public void dispatchDirect(String componentName, MuleMessage message) throws MuleException
    {
        Service service = muleContext.getRegistry().lookupService(componentName);
        if (service == null)
        {
            throw new ServiceException(CoreMessages.objectNotRegistered("Service", componentName));
        }
        InboundEndpoint endpoint = getDefaultClientEndpoint(service, message.getPayload(), false);
        MuleEvent event = new DefaultMuleEvent(message, endpoint, service);

        if (logger.isDebugEnabled())
        {
            logger.debug("MuleClient dispatching event direct to: " + componentName + ". MuleEvent is: " + event);
        }

        service.dispatchEvent(event);
    }

    /**
     * Sends an event request to a URL, making the result of the event trigger
     * available as a Future result that can be accessed later by client code.
     *
     * @param url the url to make a request on
     * @param payload the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload. as
     *            null
     * @return the result message if any of the invocation
     * @throws org.mule.api.MuleException if the dispatch fails or the components or
     *             transfromers cannot be found
     */
    public FutureMessageResult sendAsync(String url, Object payload, Map<String, Object> messageProperties)
        throws MuleException
    {
        return sendAsync(url, payload, messageProperties, 0);
    }

    /**
     * Sends an event request to a URL, making the result of the event trigger
     * available as a Future result that can be accessed later by client code.
     *
     * @param url the URL to make a request on
     * @param message the message to send
     * @return the result message if any of the invocation
     * @throws org.mule.api.MuleException if the dispatch fails or the components or
     *             transfromers cannot be found
     */
    public FutureMessageResult sendAsync(final String url, final MuleMessage message) throws MuleException
    {
        return sendAsync(url, message, MuleEvent.TIMEOUT_NOT_SET_VALUE);
    }

    /**
     * Sends an event request to a URL, making the result of the event trigger
     * available as a Future result that can be accessed later by client code.
     *
     * @param url the url to make a request on
     * @param payload the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload. as
     *            null
     * @param timeout how long to block in milliseconds waiting for a result
     * @return the result message if any of the invocation
     * @throws org.mule.api.MuleException if the dispatch fails or the components or
     *             transfromers cannot be found
     */
    public FutureMessageResult sendAsync(final String url,
                                         final Object payload,
                                         final Map<String, Object> messageProperties,
                                         final int timeout) throws MuleException
    {
        return sendAsync(url, new DefaultMuleMessage(payload, messageProperties, muleContext), timeout);
    }

    /**
     * Sends an event request to a URL, making the result of the event trigger
     * available as a Future result that can be accessed later by client code.
     *
     * @param url the url to make a request on
     * @param message the message to send
     * @param timeout how long to block in milliseconds waiting for a result
     * @return the result message if any of the invocation
     * @throws org.mule.api.MuleException if the dispatch fails or the components or
     *             transfromers cannot be found
     */
    public FutureMessageResult sendAsync(final String url, final MuleMessage message, final int timeout)
        throws MuleException
    {
        Callable<Object> call = new Callable<Object>()
        {
            @Override
            public Object call() throws Exception
            {
                return send(url, message, timeout);
            }
        };

        FutureMessageResult result = new FutureMessageResult(call, muleContext);

        if (muleContext.getWorkManager() != null)
        {
            result.setExecutor(muleContext.getWorkManager());
        }

        result.execute();
        return result;
    }

    /**
     * Sends an event to a component on a local Mule instance, while making the
     * result of the event trigger available as a Future result that can be accessed
     * later by client code. Users can specify a url to a remote Mule server in the
     * constructor of a Mule client, by default the default Mule server url
     * <code>tcp://localhost:60504</code> is used.
     *
     * @param component the name of the Mule components to send to
     * @param transformers a comma separated list of transformers to apply to the
     *            result message
     * @param payload the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload.
     * @return the result message if any of the invocation
     * @throws org.mule.api.MuleException if the dispatch fails or the components or
     *             transfromers cannot be found
     *
     * <b>Deprecated from 3.6.0</b> - Only supported with Services. Sending message to remote flows is not supported.
     */
    @Deprecated
    public FutureMessageResult sendDirectAsync(final String component,
                                               String transformers,
                                               final Object payload,
                                               final Map<String, Object> messageProperties) throws MuleException
    {
        return sendDirectAsync(component, transformers, new DefaultMuleMessage(payload, messageProperties, muleContext));
    }

    /**
     * Snds an event to a component on a local Mule instance, while making the result
     * of the event trigger available as a Future result that can be accessed later
     * by client code. Users can specify a url to a remote Mule server in the
     * constructor of a Mule client, by default the default Mule server url
     * <code>tcp://localhost:60504</code> is used.
     *
     * @param component the name of the Mule components to send to
     * @param transformers a comma separated list of transformers to apply to the
     *            result message
     * @param message the message to send
     * @return the result message if any of the invocation
     * @throws org.mule.api.MuleException if the dispatch fails or the components or
     *             transfromers cannot be found
     *
     * <b>Deprecated from 3.6.0</b> - Only supported with Services. Sending message to remote flows is not supported.
     */
    @Deprecated
    public FutureMessageResult sendDirectAsync(final String component,
                                               String transformers,
                                               final MuleMessage message) throws MuleException
    {
        Callable<Object> call = new Callable<Object>()
        {
            @Override
            public Object call() throws Exception
            {
                return sendDirect(component, null, message);
            }
        };

        FutureMessageResult result = new FutureMessageResult(call, muleContext);

        if (muleContext.getWorkManager() != null)
        {
            result.setExecutor(muleContext.getWorkManager());
        }

        if (StringUtils.isNotBlank(transformers))
        {
            result.setTransformers(TransformerUtils.getTransformers(transformers, muleContext));
        }

        result.execute();
        return result;
    }

    /**
     * Sends an event synchronously to a endpointUri via a Mule server and a
     * resulting message is returned.
     *
     * @param url the Mule URL used to determine the destination and transport of the
     *            message
     * @param payload the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload. In
     *            the case of Jms you could set the JMSReplyTo property in these
     *            properties.
     * @return A return message, this could be <code>null</code> if the the components invoked
     *         explicitly sets a return as <code>null</code>.
     * @throws org.mule.api.MuleException
     */
    public MuleMessage send(String url, Object payload, Map<String, Object> messageProperties) throws MuleException
    {
        return send(url, payload, messageProperties, MuleEvent.TIMEOUT_NOT_SET_VALUE);
    }

    /**
     * Sends an event synchronously to a endpointUri via a Mule server and a
     * resulting message is returned.
     *
     * @param url the Mule URL used to determine the destination and transport of the
     *            message
     * @param message the Message for the event
     * @return A return message, this could be <code>null</code> if the the components invoked
     *         explicitly sets a return as <code>null</code>.
     * @throws org.mule.api.MuleException
     */
    public MuleMessage send(String url, MuleMessage message) throws MuleException
    {
        return send(url, message, MuleEvent.TIMEOUT_NOT_SET_VALUE);
    }

    /**
     * Sends an event synchronously to a endpointUri via a mule server and a
     * resulting message is returned.
     *
     * @param url the Mule URL used to determine the destination and transport of the
     *            message
     * @param payload the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload. In
     *            the case of Jms you could set the JMSReplyTo property in these
     *            properties.
     * @param timeout The time in milliseconds the the call should block waiting for
     *            a response
     * @return A return message, this could be <code>null</code> if the the components invoked
     *         explicitly sets a return as <code>null</code>.
     * @throws org.mule.api.MuleException
     */
    public MuleMessage send(String url, Object payload, Map<String, Object> messageProperties, int timeout)
        throws MuleException
    {
        if (messageProperties == null)
        {
            messageProperties = new HashMap<String, Object>();
        }
        if (messageProperties.get(MuleProperties.MULE_REMOTE_SYNC_PROPERTY) == null)
        {
            // clone the map in case a call used an unmodifiable version
            messageProperties = new HashMap<String, Object>(messageProperties);
            messageProperties.put(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, "true");
        }
        MuleMessage message = new DefaultMuleMessage(payload, messageProperties, muleContext);
        return send(url, message, timeout);
    }

    /**
     * Sends an event synchronously to a endpointUri via a mule server and a
     * resulting message is returned.
     *
     * @param url the Mule URL used to determine the destination and transport of the
     *            message
     * @param message The message to send
     * @param timeout The time in milliseconds the the call should block waiting for
     *            a response
     * @return A return message, this could be <code>null</code> if the the components invoked
     *         explicitly sets a return as <code>null</code>.
     * @throws org.mule.api.MuleException
     */
    public MuleMessage send(String url, MuleMessage message, int timeout) throws MuleException
    {
        OutboundEndpoint endpoint =
            getOutboundEndpoint(url, MessageExchangePattern.REQUEST_RESPONSE, timeout);

        MuleEvent event = getEvent(message, MessageExchangePattern.REQUEST_RESPONSE);

        MuleEvent response = endpoint.process(event);
        if (response != null && !VoidMuleEvent.getInstance().equals(response))
        {
            return response.getMessage();
        }
        else
        {
            return new DefaultMuleMessage(NullPayload.getInstance(), muleContext);
        }
    }

    /**
     * Will receive an event from an endpointUri determined by the URL.
     *
     * @param url the Mule URL used to determine the destination and transport of the
     *            message
     * @param timeout how long to block waiting to receive the event, if set to 0 the
     *            receive will not wait at all and if set to -1 the receive will wait
     *            forever
     * @return the message received or <code>null</code> if no message was received
     * @throws org.mule.api.MuleException
     */
    public MuleMessage request(String url, long timeout) throws MuleException
    {
        InboundEndpoint endpoint = getInboundEndpoint(url);
        try
        {
            return endpoint.request(timeout);
        }
        catch (Exception e)
        {
            throw new ReceiveException(endpoint, timeout, e);
        }
    }

    /**
     * Will receive an event from an endpointUri determined by the URL
     *
     * @param url the Mule URL used to determine the destination and transport of the
     *            message
     * @param transformers A comma separated list of transformers used to apply to
     *            the result message
     * @param timeout how long to block waiting to receive the event, if set to 0 the
     *            receive will not wait at all and if set to -1 the receive will wait
     *            forever
     * @return the message received or <code>null</code> if no message was received
     * @throws org.mule.api.MuleException
     */
    public MuleMessage request(String url, String transformers, long timeout) throws MuleException
    {
        return request(url, TransformerUtils.getTransformers(transformers, muleContext), timeout);
    }

    /**
     * Will receive an event from an endpointUri determined by the URL
     *
     * @param url the Mule URL used to determine the destination and transport of the
     *            message
     * @param transformers Transformers used to modify the result message
     * @param timeout how long to block waiting to receive the event, if set to 0 the
     *            receive will not wait at all and if set to -1 the receive will wait
     *            forever
     * @return the message received or <code>null</code> if no message was received
     * @throws org.mule.api.MuleException
     */
    public MuleMessage request(String url, List<?> transformers, long timeout) throws MuleException
    {
        return request(url, timeout);
    }

    protected MuleEvent getEvent(MuleMessage message, MessageExchangePattern exchangePattern)
        throws MuleException
    {
        if (user != null)
        {
            message.setOutboundProperty(MuleProperties.MULE_USER_PROPERTY,
                MuleCredentials.createHeader(user.getUsername(), user.getPassword()));
        }
        return new DefaultMuleEvent(message, exchangePattern,
            new DefaultLocalMuleClient.MuleClientFlowConstruct(muleContext));
    }

    protected InboundEndpoint getInboundEndpoint(String uri) throws MuleException
    {
        // There was a potential leak here between get() and putIfAbsent(). This
        // would cause the endpoint that was created to be used rather an endpoint
        // with the same key that has been created and put in the cache by another
        // thread. To avoid this we test for the result of putIfAbsent result and if
        // it is non-null then an endpoint was created and added concurrently and we
        // return this instance instead.
        InboundEndpoint endpoint = inboundEndpointCache.get(uri);
        if (endpoint == null)
        {
            endpoint = muleContext.getEndpointFactory().getInboundEndpoint(uri);
            InboundEndpoint concurrentlyAddedEndpoint = inboundEndpointCache.putIfAbsent(uri, endpoint);
            if (concurrentlyAddedEndpoint != null)
            {
                return concurrentlyAddedEndpoint;
            }
        }
        return endpoint;
    }

    protected OutboundEndpoint getOutboundEndpoint(String uri, MessageExchangePattern exchangePattern,
        Integer responseTimeout) throws MuleException
    {
        // There was a potential leak here between get() and putIfAbsent(). This
        // would cause the endpoint that was created to be used rather an endpoint
        // with the same key that has been created and put in the cache by another
        // thread. To avoid this we test for the result of putIfAbsent result and if
        // it is non-null then an endpoint was created and added concurrently and we
        // return this instance instead.
        String key = String.format("%1s:%2s:%3s", uri, exchangePattern, responseTimeout);
        OutboundEndpoint endpoint = outboundEndpointCache.get(key);
        if (endpoint == null)
        {
            EndpointBuilder endpointBuilder =
                muleContext.getEndpointFactory().getEndpointBuilder(uri);
            endpointBuilder.setExchangePattern(exchangePattern);
            if (responseTimeout != null && responseTimeout > 0)
            {
                endpointBuilder.setResponseTimeout(responseTimeout.intValue());
            }
            endpoint = muleContext.getEndpointFactory().getOutboundEndpoint(endpointBuilder);
            OutboundEndpoint concurrentlyAddedEndpoint =
                outboundEndpointCache.putIfAbsent(key, endpoint);
            if (concurrentlyAddedEndpoint != null)
            {
                return concurrentlyAddedEndpoint;
            }
        }
        return endpoint;
    }

    protected InboundEndpoint getDefaultClientEndpoint(Service service, Object payload, boolean sync)
        throws MuleException
    {
        if (!(service.getMessageSource() instanceof ServiceCompositeMessageSource))
        {
            throw new IllegalStateException(
                "Only 'CompositeMessageSource' is supported with MuleClient.sendDirect() and MuleClient.dispatchDirect()");
        }

        // as we are bypassing the message transport layer we need to check that
        InboundEndpoint endpoint = ((ServiceCompositeMessageSource) service.getMessageSource()).getEndpoints().get(0);
        if (endpoint != null)
        {
            if (endpoint.getTransformers() != null)
            {
                // the original code here really did just check the first exception
                // as far as i can tell
                if (TransformerUtils.isSourceTypeSupportedByFirst(endpoint.getTransformers(),
                    payload.getClass()))
                {
                    return endpoint;
                }
                else
                {
                    EndpointBuilder builder = new EndpointURIEndpointBuilder(endpoint);
                    builder.setTransformers(new LinkedList());
                    builder.setExchangePattern(MessageExchangePattern.REQUEST_RESPONSE);
                    return muleContext.getEndpointFactory().getInboundEndpoint(builder);
                }
            }
            else
            {
                return endpoint;
            }
        }
        else
        {
            EndpointBuilder builder = new EndpointURIEndpointBuilder("vm://mule.client", muleContext);
            builder.setName("muleClientProvider");
            endpoint = muleContext.getEndpointFactory().getInboundEndpoint(builder);
        }
        return endpoint;
    }

    /**
     * Sends an event synchronously to a endpointUri via a Mule server without
     * waiting for the result.
     *
     * @param url the Mule URL used to determine the destination and transport of the
     *            message
     * @param payload the object that is the payload of the event
     * @param messageProperties any properties to be associated with the payload. In
     *            the case of Jms you could set the JMSReplyTo property in these
     *            properties.
     * @throws org.mule.api.MuleException
     */
    public void sendNoReceive(String url, Object payload, Map<String, Object> messageProperties) throws MuleException
    {
        if (messageProperties == null)
        {
            messageProperties = new HashMap<String, Object>();
        }
        messageProperties.put(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, "false");
        MuleMessage message = new DefaultMuleMessage(payload, messageProperties, muleContext);

        OutboundEndpoint endpoint =
            getOutboundEndpoint(url, MessageExchangePattern.REQUEST_RESPONSE, null);
        MuleEvent event = getEvent(message, MessageExchangePattern.REQUEST_RESPONSE);
        endpoint.process(event);
    }

    /**
     * The overriding method may want to return a custom {@link MuleContext} here
     *
     * @return the MuleContext to use
     */
    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    /**
     * Registers a Java object as a component that listens for events on the
     * given URL. By default the ThreadingProfile for the components will be set so
     * that there will only be one thread of execution.
     *
     * @param component any java object, Mule will it's endpointUri discovery to
     *            determine which event to invoke based on the evnet payload type
     * @param name The identifying name of the components. This can be used to later
     *            unregister it
     * @param listenerEndpoint The url endpointUri to listen to
     * @throws MuleException
     * @deprecated Use the RegistryContext to get the registry and register the
     *             service there
     */
    @Deprecated
    public void registerComponent(Object component, String name, EndpointURI listenerEndpoint)
        throws MuleException
    {
        throw new UnsupportedOperationException("registerComponent");
    }

    /**
     * Registers a Java object as a component that listens for and sends events
     * on the given urls. By default the ThreadingProfile for the components will be
     * set so that there will only be one thread of execution.
     *
     * @param component any java object, Mule will it's endpointUri discovery to
     *            determine which event to invoke based on the evnet payload type
     * @param name The identifying name of the components. This can be used to later
     *            unregister it
     * @param listenerEndpoint The url endpointUri to listen to
     * @param sendEndpoint The url endpointUri to dispatch to
     * @throws MuleException
     * @deprecated Use the RegistryContext to get the registry and register the
     *             service there
     */
    @Deprecated
    public void registerComponent(Object component,
                                  String name,
                                  MuleEndpointURI listenerEndpoint,
                                  MuleEndpointURI sendEndpoint) throws MuleException
    {
        throw new UnsupportedOperationException("registerComponent");
    }

    /**
     * Unregisters a previously register components. This will also unregister any
     * listeners for the components Calling this method is equivilent to calling
     * Model.unregisterComponent(..)
     *
     * @param name the name of the componet to unregister
     * @throws MuleException if unregistering the components fails, i.e. The
     *             underlying transport fails to unregister a listener. If the
     *             components does not exist, this method should not throw an
     *             exception.
     * @see org.mule.api.model.Model
     * @deprecated Use the RegistryContext to get the registry and unregister the
     *             service there
     */
    @Deprecated
    public void unregisterComponent(String name) throws MuleException
    {
        throw new UnsupportedOperationException("registerComponent");
    }

    @Deprecated
    public RemoteDispatcher getRemoteDispatcher(String serverEndpoint) throws MuleException
    {
        RemoteDispatcher rd = new RemoteDispatcher(serverEndpoint, muleContext);
        rd.setExecutor(muleContext.getWorkManager());
        dispatchers.add(rd);
        return rd;
    }

    @Deprecated
    public RemoteDispatcher getRemoteDispatcher(String serverEndpoint, String user, String password)
        throws MuleException
    {
        RemoteDispatcher rd = new RemoteDispatcher(serverEndpoint, new MuleCredentials(user,
            password.toCharArray()), muleContext);
        rd.setExecutor(muleContext.getWorkManager());
        dispatchers.add(rd);
        return rd;
    }

    /**
     * Will dispose the MuleManager instance <b>if</b> a new instance was created for this
     * client. Otherwise this method only cleans up resources no longer needed
     */
    @Override
    public void dispose()
    {
        synchronized (dispatchers)
        {
            for (RemoteDispatcher remoteDispatcher : dispatchers)
            {
                remoteDispatcher.dispose();
            }
            dispatchers.clear();
        }
        // Dispose the muleContext only if the muleContext was created for this
        // client
        if (muleContext.getConfiguration().isClientMode())
        {
            logger.info("Stopping Mule...");
            muleContext.dispose();
        }
    }

    public void setProperty(String key, Object value)
    {
        try
        {
            muleContext.getRegistry().registerObject(key, value);
        }
        catch (RegistrationException e)
        {
            logger.error(e);
        }
    }

    public Object getProperty(String key)
    {
        return muleContext.getRegistry().lookupObject(key);
    }

    public MuleConfiguration getConfiguration()
    {
        return muleContext.getConfiguration();
    }
}
