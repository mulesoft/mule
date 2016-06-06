/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transport.service;

import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.EndpointException;
import org.mule.compatibility.core.api.endpoint.EndpointURIBuilder;
import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.transformer.EndpointAwareTransformer;
import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.api.transport.MessageDispatcherFactory;
import org.mule.compatibility.core.api.transport.MessageReceiver;
import org.mule.compatibility.core.api.transport.MessageRequesterFactory;
import org.mule.compatibility.core.api.transport.MuleMessageFactory;
import org.mule.compatibility.core.config.i18n.TransportCoreMessages;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.compatibility.core.endpoint.UrlEndpointURIBuilder;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.registry.AbstractServiceDescriptor;
import org.mule.runtime.core.api.transaction.TransactionFactory;
import org.mule.runtime.core.api.transformer.MessageTransformer;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.message.SessionHandler;
import org.mule.runtime.core.session.SerializeAndEncodeSessionHandler;
import org.mule.runtime.core.transaction.XaTransactionFactory;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.CollectionUtils;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.core.util.SystemUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

;

public class DefaultTransportServiceDescriptor extends AbstractServiceDescriptor implements TransportServiceDescriptor
{
    private String connector;
    private String dispatcherFactory;
    private String requesterFactory;
    private String transactionFactory;
    private String messageFactory;
    private String messageReceiver;
    private String transactedMessageReceiver;
    private String xaTransactedMessageReceiver;
    private String endpointUriBuilder;
    private String sessionHandler;
    private String defaultInboundTransformer;
    private String defaultOutboundTransformer;
    private String defaultResponseTransformer;
    private String endpointBuilder;

    private Properties exceptionMappings = new Properties();
    MuleContext muleContext;
    private List<MessageExchangePattern> inboundExchangePatterns;
    private List<MessageExchangePattern> outboundExchangePatterns;
    private String defaultExchangePattern;

    private ClassLoader classLoader;

    public DefaultTransportServiceDescriptor(String service, Properties props, ClassLoader classLoader)
    {
        super(service);
        this.classLoader = classLoader;
        init(props);
    }

    protected void init(Properties props)
    {
        connector = removeProperty(MuleProperties.CONNECTOR_CLASS, props);
        dispatcherFactory = removeProperty(MuleProperties.CONNECTOR_DISPATCHER_FACTORY, props);
        requesterFactory = removeProperty(MuleProperties.CONNECTOR_REQUESTER_FACTORY, props);
        transactionFactory = removeProperty(MuleProperties.CONNECTOR_DISPATCHER_FACTORY, props);
        messageReceiver = removeProperty(MuleProperties.CONNECTOR_MESSAGE_RECEIVER_CLASS, props);
        transactedMessageReceiver = removeProperty(MuleProperties.CONNECTOR_TRANSACTED_MESSAGE_RECEIVER_CLASS, props);
        xaTransactedMessageReceiver = removeProperty(MuleProperties.CONNECTOR_XA_TRANSACTED_MESSAGE_RECEIVER_CLASS, props);
        messageFactory = removeProperty(MuleProperties.CONNECTOR_MESSAGE_FACTORY, props);
        defaultInboundTransformer = removeProperty(MuleProperties.CONNECTOR_INBOUND_TRANSFORMER, props);
        defaultOutboundTransformer = removeProperty(MuleProperties.CONNECTOR_OUTBOUND_TRANSFORMER, props);
        defaultResponseTransformer = removeProperty(MuleProperties.CONNECTOR_RESPONSE_TRANSFORMER, props);
        endpointBuilder = removeProperty(MuleProperties.CONNECTOR_META_ENDPOINT_BUILDER, props);
        endpointUriBuilder = removeProperty(MuleProperties.CONNECTOR_ENDPOINT_BUILDER, props);
        sessionHandler = removeProperty(MuleProperties.CONNECTOR_SESSION_HANDLER, props);

        initInboundExchangePatterns(props);
        initOutboundExchangePatterns(props);
        defaultExchangePattern = removeProperty(MuleProperties.CONNECTOR_DEFAULT_EXCHANGE_PATTERN, props);
    }

    public void setOverrides(Properties props)
    {
        if (props == null || props.size() == 0)
        {
            return;
        }

        connector = props.getProperty(MuleProperties.CONNECTOR_CLASS, connector);
        dispatcherFactory = props.getProperty(MuleProperties.CONNECTOR_DISPATCHER_FACTORY, dispatcherFactory);
        requesterFactory = props.getProperty(MuleProperties.CONNECTOR_REQUESTER_FACTORY, requesterFactory);
        messageReceiver = props.getProperty(MuleProperties.CONNECTOR_MESSAGE_RECEIVER_CLASS, messageReceiver);
        transactedMessageReceiver = props.getProperty(
                MuleProperties.CONNECTOR_TRANSACTED_MESSAGE_RECEIVER_CLASS, transactedMessageReceiver);
        xaTransactedMessageReceiver = props.getProperty(
                MuleProperties.CONNECTOR_XA_TRANSACTED_MESSAGE_RECEIVER_CLASS, xaTransactedMessageReceiver);
        messageFactory = props.getProperty(MuleProperties.CONNECTOR_MESSAGE_FACTORY, messageFactory);
        endpointBuilder = props.getProperty(MuleProperties.CONNECTOR_META_ENDPOINT_BUILDER, endpointBuilder);

        String temp = props.getProperty(MuleProperties.CONNECTOR_INBOUND_TRANSFORMER);
        if (temp != null)
        {
            defaultInboundTransformer = temp;
        }

        temp = props.getProperty(MuleProperties.CONNECTOR_OUTBOUND_TRANSFORMER);
        if (temp != null)
        {
            defaultOutboundTransformer = temp;
        }

        temp = props.getProperty(MuleProperties.CONNECTOR_RESPONSE_TRANSFORMER);
        if (temp != null)
        {
            defaultResponseTransformer = temp;
        }

        temp = props.getProperty(MuleProperties.CONNECTOR_ENDPOINT_BUILDER);
        if (temp != null)
        {
            endpointUriBuilder = temp;
        }

        initInboundExchangePatterns(props);
        initOutboundExchangePatterns(props);
        defaultExchangePattern = props.getProperty(MuleProperties.CONNECTOR_DEFAULT_EXCHANGE_PATTERN, null);
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    @Override
    public MuleMessageFactory createMuleMessageFactory() throws TransportServiceException
    {
        if (messageFactory == null)
        {
            throw new TransportServiceException(TransportCoreMessages.objectNotSetInService("Message Factory",
                getService()));
        }

        try
        {
            return (MuleMessageFactory) ClassUtils.instanciateClass(messageFactory, null, classLoader);
        }
        catch (NoSuchMethodException nsme)
        {
            //For backward compatibility keep trying to use deprecated constructor for custom message factories.
            logger.warn(String.format("Couldn't find %s empty constructor. " +
                                      "%s must be updated to have an empty constructor in order to work properly within domains.",
                                      messageFactory, messageFactory));
            try
            {
                final Object[] args = new Object[] { muleContext };
                return (MuleMessageFactory) ClassUtils.instanciateClass(messageFactory, args, classLoader);
            }
            catch (Exception e)
            {
                throw new TransportServiceException(CoreMessages.failedToCreate("Message Factory"), e);
            }
        }
        catch (Exception e)
        {
            throw new TransportServiceException(CoreMessages.failedToCreate("Message Factory"), e);
        }
    }

    @Override
    public SessionHandler createSessionHandler() throws TransportServiceException
    {
        if (sessionHandler == null)
        {
            sessionHandler = SerializeAndEncodeSessionHandler.class.getName();
            if (logger.isDebugEnabled())
            {
                logger.debug("No session.handler set in service description, defaulting to: "
                        + sessionHandler);
            }
        }
        try
        {
            return (SessionHandler) ClassUtils.instanciateClass(sessionHandler, ClassUtils.NO_ARGS, classLoader);
        }
        catch (Throwable e)
        {
            throw new TransportServiceException(TransportCoreMessages.failedToCreateObjectWith("SessionHandler", sessionHandler), e);
        }
    }

    @Override
    public MessageReceiver createMessageReceiver(Connector connector,
                                                 FlowConstruct flowConstruct,
                                                 InboundEndpoint endpoint) throws MuleException
    {

        MessageReceiver mr = createMessageReceiver(connector, flowConstruct, endpoint, null);
        return mr;
    }

    @Override
    public MessageReceiver createMessageReceiver(Connector connector,
                                                 FlowConstruct flowConstruct,
                                                 InboundEndpoint endpoint,
                                                 Object... args) throws MuleException
    {
        String receiverClass = messageReceiver;

        if (endpoint.getTransactionConfig().isTransacted())
        {
            boolean xaTx = endpoint.getTransactionConfig().getFactory() instanceof XaTransactionFactory;
            if (transactedMessageReceiver != null && !xaTx)
            {
                receiverClass = transactedMessageReceiver;
            }
            else if (xaTransactedMessageReceiver != null && xaTx)
            {
                receiverClass = xaTransactedMessageReceiver;
            }

        }

        if (receiverClass != null)
        {
            Object[] newArgs;

            if (args != null && args.length != 0)
            {
                newArgs = new Object[3 + args.length];
            }
            else
            {
                newArgs = new Object[3];
            }

            newArgs[0] = connector;
            newArgs[1] = flowConstruct;
            newArgs[2] = endpoint;

            if (args != null && args.length != 0)
            {
                System.arraycopy(args, 0, newArgs, 3, newArgs.length - 3);
            }

            try
            {
                MessageReceiver mr = (MessageReceiver) ClassUtils.instanciateClass(receiverClass, newArgs, classLoader);
                return mr;
            }
            catch (Exception e)
            {
                throw new TransportServiceException(TransportCoreMessages.failedToCreateObjectWith("Message Receiver", getService()), e);
            }

        }
        else
        {
            throw new TransportServiceException(TransportCoreMessages.objectNotSetInService("Message Receiver", getService()));
        }
    }

    @Override
    public MessageDispatcherFactory createDispatcherFactory() throws TransportServiceException
    {
        if (dispatcherFactory != null)
        {
            try
            {
                return (MessageDispatcherFactory) ClassUtils.instanciateClass(dispatcherFactory,
                        ClassUtils.NO_ARGS, classLoader);
            }
            catch (Exception e)
            {
                throw new TransportServiceException(TransportCoreMessages.failedToCreateObjectWith("Message Dispatcher Factory", dispatcherFactory), e);
            }
        }
        else
        {
            //Its valid not to have a Dispatcher factory on the transport
            return null;
        }
    }

    @Override
    public MessageRequesterFactory createRequesterFactory() throws TransportServiceException
    {
        if (requesterFactory != null)
        {
            try
            {
                return (MessageRequesterFactory) ClassUtils.instanciateClass(requesterFactory,
                        ClassUtils.NO_ARGS, classLoader);
            }
            catch (Exception e)
            {
                throw new TransportServiceException(
                        TransportCoreMessages.failedToCreateObjectWith("Message Requester Factory", requesterFactory), e);
            }
        }
        else
        {
            //Its valid not to have a Requester factory on the transport
            return null;
        }
    }

    @Override
    public TransactionFactory createTransactionFactory() throws TransportServiceException
    {
        if (transactionFactory != null)
        {
            try
            {
                return (TransactionFactory) ClassUtils.instanciateClass(transactionFactory,
                        ClassUtils.NO_ARGS, classLoader);
            }
            catch (Exception e)
            {
                throw new TransportServiceException(TransportCoreMessages.failedToCreateObjectWith("Transaction Factory", transactionFactory), e);
            }
        }
        else
        {
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Connector createConnector() throws TransportServiceException
    {
        Connector newConnector;
        // if there is a factory, use it
        try
        {
            if (connector != null)
            {
                Class<Connector> connectorClass;
                if (classLoader != null)
                {
                    connectorClass = ClassUtils.loadClass(connector, classLoader);
                }
                else
                {
                    connectorClass = ClassUtils.loadClass(connector, getClass());
                }
                newConnector = connectorClass.getConstructor(MuleContext.class).newInstance(muleContext);
            }
            else
            {
                throw new TransportServiceException(TransportCoreMessages.objectNotSetInService("Connector", getService()));
            }
        }
        catch (TransportServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new TransportServiceException(TransportCoreMessages.failedToCreateObjectWith("Connector", connector), e);
        }

        if (newConnector.getName() == null)
        {
            newConnector.setName("_" + newConnector.getProtocol() + "Connector#" + connector.hashCode());
        }
        return newConnector;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Transformer> createInboundTransformers(ImmutableEndpoint endpoint) throws TransportFactoryException
    {
        if (defaultInboundTransformer != null)
        {
            logger.info("Loading default inbound transformer: " + defaultInboundTransformer);
            try
            {
                Transformer newTransformer = createTransformer(defaultInboundTransformer, endpoint);
                return CollectionUtils.singletonList(newTransformer);
            }
            catch (Exception e)
            {
                throw new TransportFactoryException(CoreMessages.failedToLoadTransformer("inbound",
                    defaultInboundTransformer), e);
            }
        }
        return Collections.emptyList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Transformer> createOutboundTransformers(ImmutableEndpoint endpoint) throws TransportFactoryException
    {
        if (defaultOutboundTransformer != null)
        {
            logger.info("Loading default outbound transformer: " + defaultOutboundTransformer);
            try
            {
                Transformer newTransformer = createTransformer(defaultOutboundTransformer, endpoint);
                return CollectionUtils.singletonList(newTransformer);
            }
            catch (Exception e)
            {
                throw new TransportFactoryException(CoreMessages.failedToLoadTransformer("outbound",
                    defaultOutboundTransformer), e);
            }
        }
        return Collections.emptyList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Transformer> createResponseTransformers(ImmutableEndpoint endpoint) throws TransportFactoryException
    {
        if (defaultResponseTransformer != null)
        {
            logger.info("Loading default response transformer: " + defaultResponseTransformer);
            try
            {
                Transformer newTransformer = createTransformer(defaultResponseTransformer, endpoint);
                return CollectionUtils.singletonList(newTransformer);
            }
            catch (Exception e)
            {
                throw new TransportFactoryException(CoreMessages.failedToLoadTransformer("response",
                    defaultResponseTransformer), e);
            }
        }
        return Collections.emptyList();
    }

    protected Transformer createTransformer(String className, ImmutableEndpoint endpoint) throws Exception
    {
        final Transformer innerTransformer = (Transformer) ClassUtils.instanciateClass(className, ClassUtils.NO_ARGS, classLoader);

        EndpointAwareTransformer newTransformer;
        if (innerTransformer instanceof MessageTransformer)
        {
            newTransformer = new DefaultEndpointAwareMessageTransformer(
                    (MessageTransformer) innerTransformer, SystemUtils.getDefaultEncoding(muleContext));
        }
        else
        {
            newTransformer = new DefaultEndpointAwareTransformer(
                    innerTransformer, SystemUtils.getDefaultEncoding(muleContext));
        }

        newTransformer.setMuleContext(muleContext);
        newTransformer.setName(newTransformer.getName() + "#" + newTransformer.hashCode());
        newTransformer.setEndpoint(endpoint);
        return newTransformer;
    }

    @Override
    public EndpointURIBuilder createEndpointURIBuilder() throws TransportFactoryException
    {
        if (endpointUriBuilder == null)
        {
            logger.debug("Endpoint resolver not set, Loading default resolver: "
                    + UrlEndpointURIBuilder.class.getName());
            return new UrlEndpointURIBuilder();
        }
        else
        {
            logger.debug("Loading endpointUri resolver: " + endpointUriBuilder);
            try
            {
                return (EndpointURIBuilder) ClassUtils.instanciateClass(endpointUriBuilder, ClassUtils.NO_ARGS, classLoader);
            }
            catch (Exception e)
            {
                throw new TransportFactoryException(CoreMessages.failedToLoad("EndpointURI Builder: " + endpointUriBuilder), e);
            }
        }
    }

    @Override
    public EndpointBuilder createEndpointBuilder(String uri) throws TransportFactoryException
    {
         return createEndpointBuilder(uri, muleContext);
    }

    @Override
    public EndpointBuilder createEndpointBuilder(String uri, MuleContext muleContext) throws TransportFactoryException
    {
        if (endpointBuilder == null)
        {
            logger.debug("Endpoint builder not set, Loading default builder: "
                         + EndpointURIEndpointBuilder.class.getName());
            return new EndpointURIEndpointBuilder(uri, muleContext);
        }
        else
        {
            return createEndpointBuilder(new Object[] { uri, muleContext });
        }
    }

    @Override
    public EndpointBuilder createEndpointBuilder(EndpointURIEndpointBuilder builder) throws TransportFactoryException
    {
        return createEndpointBuilder(builder, muleContext);
    }

    @Override
    public EndpointBuilder createEndpointBuilder(EndpointURIEndpointBuilder builder, MuleContext muleContext) throws TransportFactoryException
    {
        EndpointBuilder wrappingBuilder;
        if (endpointBuilder == null)
        {
            logger.debug("Endpoint builder not set, Loading default builder: "
                    + EndpointURIEndpointBuilder.class.getName());
            try
            {
                wrappingBuilder = new EndpointURIEndpointBuilder(builder);
            }
            catch (EndpointException e)
            {
                throw new TransportFactoryException(CoreMessages.failedToLoad("Endpoint Builder: " + endpointBuilder), e);
            }
        }
        else
        {
            wrappingBuilder = createEndpointBuilder(new Object[] { builder });
        }

        wrappingBuilder.setMuleContext(muleContext);
        return wrappingBuilder;
    }

    protected EndpointBuilder createEndpointBuilder(Object[] constructorParams) throws TransportFactoryException
    {
        logger.debug("Loading endpoint builder: " + endpointBuilder);
        try
        {
            return (EndpointBuilder) ClassUtils.instanciateClass(endpointBuilder, constructorParams, classLoader);
        }
        catch (Exception e)
        {
            throw new TransportFactoryException(CoreMessages.failedToLoad("Endpoint Builder: " + endpointBuilder), e);
        }
    }

    @Override
    public void setExceptionMappings(Properties props)
    {
        this.exceptionMappings = props;
    }

    @Override
    public Properties getExceptionMappings()
    {
        return this.exceptionMappings;
    }

    protected void initInboundExchangePatterns(Properties properties)
    {
        // it's valid to configure no inbound exchange patterns but it's invalid to have
        // no key for inbound exchange patterns
        if (!properties.keySet().contains(MuleProperties.CONNECTOR_INBOUND_EXCHANGE_PATTERNS))
        {
            inboundExchangePatterns = null;
        }
        else
        {
            String mepsString =
                removeProperty(MuleProperties.CONNECTOR_INBOUND_EXCHANGE_PATTERNS, properties);
            inboundExchangePatterns = parseExchangePatterns(mepsString);
        }
    }

    protected void initOutboundExchangePatterns(Properties properties)
    {
        // it's valid to configure no outbound exchange patterns but it's invalid to have
        // no key for outbound exchange patterns
        if (!properties.keySet().contains(MuleProperties.CONNECTOR_OUTBOUND_EXCHANGE_PATTERNS))
        {
            outboundExchangePatterns = null;
        }
        else
        {
            String mepsString =
                removeProperty(MuleProperties.CONNECTOR_OUTBOUND_EXCHANGE_PATTERNS, properties);
            outboundExchangePatterns = parseExchangePatterns(mepsString);
        }
    }

    protected List<MessageExchangePattern> parseExchangePatterns(String mepsString)
    {
        if (StringUtils.isEmpty(mepsString))
        {
            return Collections.emptyList();
        }

        List<MessageExchangePattern> mepList = new ArrayList<MessageExchangePattern>();

        String[] meps = StringUtils.splitAndTrim(mepsString, ",");
        for (String exchangePattern : meps)
        {
            mepList.add(MessageExchangePattern.fromString(exchangePattern));
        }

        return mepList;
    }

    @Override
    public List<MessageExchangePattern> getInboundExchangePatterns() throws TransportServiceException
    {
        if (inboundExchangePatterns == null)
        {
            throw new TransportServiceException(TransportCoreMessages.objectNotSetInService(
                MuleProperties.CONNECTOR_INBOUND_EXCHANGE_PATTERNS, getService()));
        }
        return inboundExchangePatterns;
    }

    @Override
    public List<MessageExchangePattern> getOutboundExchangePatterns() throws TransportServiceException
    {
        if (outboundExchangePatterns == null)
        {
            throw new TransportServiceException(TransportCoreMessages.objectNotSetInService(
                MuleProperties.CONNECTOR_OUTBOUND_EXCHANGE_PATTERNS, getService()));
        }
        return outboundExchangePatterns;
    }

    @Override
    public MessageExchangePattern getDefaultExchangePattern() throws TransportServiceException
    {
        if (defaultExchangePattern == null)
        {
            throw new TransportServiceException(TransportCoreMessages.objectNotSetInService(
                MuleProperties.CONNECTOR_DEFAULT_EXCHANGE_PATTERN, getService()));
        }

        return MessageExchangePattern.fromString(defaultExchangePattern);
    }
}
