/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.service;

import org.mule.config.MuleProperties;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.MuleSessionHandler;
import org.mule.impl.endpoint.EndpointURIBuilder;
import org.mule.impl.endpoint.UrlEndpointURIBuilder;
import org.mule.providers.NullPayload;
import org.mule.registry.AbstractServiceDescriptor;
import org.mule.registry.Registry;
import org.mule.transaction.XaTransactionFactory;
import org.mule.transformers.TransformerUtils;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.UMOTransactionFactory;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.provider.UMOMessageDispatcherFactory;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.umo.provider.UMOSessionHandler;
import org.mule.umo.transformer.DiscoverableTransformer;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.ClassUtils;
import org.mule.util.CollectionUtils;
import org.mule.util.object.ObjectFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/** @inheritDocs */
public class DefaultTransportServiceDescriptor extends AbstractServiceDescriptor implements TransportServiceDescriptor
{
    private String connector;
    private String connectorFactory;
    private String dispatcherFactory;
    private String transactionFactory;
    private String messageAdapter;
    private String messageReceiver;
    private String transactedMessageReceiver;
    private String xaTransactedMessageReceiver;
    private String endpointBuilder;
    private String sessionHandler;
    private String defaultInboundTransformer;
    private String defaultOutboundTransformer;
    private String defaultResponseTransformer;

    private UMOTransformer inboundTransformer;
    private UMOTransformer outboundTransformer;
    private UMOTransformer responseTransformer;
    // private EndpointBuilder endpointBuilderImpl;

    private Properties exceptionMappings = new Properties();

    public DefaultTransportServiceDescriptor(String service, Properties props, Registry registry)
    {
        super(service);

        connector = removeProperty(MuleProperties.CONNECTOR_CLASS, props);
        connectorFactory = removeProperty(MuleProperties.CONNECTOR_FACTORY, props);
        dispatcherFactory = removeProperty(MuleProperties.CONNECTOR_DISPATCHER_FACTORY, props);
        transactionFactory = removeProperty(MuleProperties.CONNECTOR_DISPATCHER_FACTORY, props);
        messageReceiver = removeProperty(MuleProperties.CONNECTOR_MESSAGE_RECEIVER_CLASS, props);
        transactedMessageReceiver = removeProperty(MuleProperties.CONNECTOR_TRANSACTED_MESSAGE_RECEIVER_CLASS, props);
        xaTransactedMessageReceiver = removeProperty(MuleProperties.CONNECTOR_XA_TRANSACTED_MESSAGE_RECEIVER_CLASS, props);
        messageAdapter = removeProperty(MuleProperties.CONNECTOR_MESSAGE_ADAPTER, props);
        defaultInboundTransformer = removeProperty(MuleProperties.CONNECTOR_INBOUND_TRANSFORMER, props);
        defaultOutboundTransformer = removeProperty(MuleProperties.CONNECTOR_OUTBOUND_TRANSFORMER, props);
        defaultResponseTransformer = removeProperty(MuleProperties.CONNECTOR_RESPONSE_TRANSFORMER, props);
        endpointBuilder = removeProperty(MuleProperties.CONNECTOR_ENDPOINT_BUILDER, props);
        sessionHandler = removeProperty(MuleProperties.CONNECTOR_SESSION_HANDLER, props);

//        try
//        {
//                registerDefaultTransformers(registry);
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//        }
    }


    private void registerDefaultTransformers(Registry registry) throws UMOException
    {
        doRegisterTransformers(createInboundTransformers(), registry);
        doRegisterTransformers(createOutboundTransformers(), registry);
        doRegisterTransformers(createResponseTransformers(), registry);
    }

    private void doRegisterTransformers(List trans, Registry registry) throws UMOException
    {
        if (trans == null || TransformerUtils.isUndefined(trans))
        {
            return;
        }

        for (Iterator iterator = trans.iterator(); iterator.hasNext();)
        {
            UMOTransformer transformer = (UMOTransformer) iterator.next();
            if (transformer instanceof DiscoverableTransformer)
            {
                registry.registerTransformer(transformer);
            }
            else
            {
                logger.warn("Transformer does not implement the DiscoverableTransformer interface, so will not be " +
                        "registered as a default implementation with the Registry. Transformr is: " + transformer);
            }
        }
    }

    public void setOverrides(Properties props)
    {
        if (props == null || props.size() == 0)
        {
            return;
        }

        connector = props.getProperty(MuleProperties.CONNECTOR_CLASS, connector);
        connectorFactory = props.getProperty(MuleProperties.CONNECTOR_FACTORY, connectorFactory);
        dispatcherFactory = props.getProperty(MuleProperties.CONNECTOR_DISPATCHER_FACTORY, dispatcherFactory);
        messageReceiver = props.getProperty(MuleProperties.CONNECTOR_MESSAGE_RECEIVER_CLASS, messageReceiver);
        transactedMessageReceiver = props.getProperty(
                MuleProperties.CONNECTOR_TRANSACTED_MESSAGE_RECEIVER_CLASS, transactedMessageReceiver);
        xaTransactedMessageReceiver = props.getProperty(
                MuleProperties.CONNECTOR_XA_TRANSACTED_MESSAGE_RECEIVER_CLASS, xaTransactedMessageReceiver);
        messageAdapter = props.getProperty(MuleProperties.CONNECTOR_MESSAGE_ADAPTER, messageAdapter);

        String temp = props.getProperty(MuleProperties.CONNECTOR_INBOUND_TRANSFORMER);
        if (temp != null)
        {
            defaultInboundTransformer = temp;
            inboundTransformer = null;
        }

        temp = props.getProperty(MuleProperties.CONNECTOR_OUTBOUND_TRANSFORMER);
        if (temp != null)
        {
            defaultOutboundTransformer = temp;
            outboundTransformer = null;
        }

        temp = props.getProperty(MuleProperties.CONNECTOR_RESPONSE_TRANSFORMER);
        if (temp != null)
        {
            defaultResponseTransformer = temp;
            responseTransformer = null;
        }

        temp = props.getProperty(MuleProperties.CONNECTOR_ENDPOINT_BUILDER);
        if (temp != null)
        {
            endpointBuilder = temp;
        }
    }

    /* (non-Javadoc)
     * @see org.mule.providers.service.TransportServiceDescriptor#createMessageAdapter(java.lang.Object)
     */
    public UMOMessageAdapter createMessageAdapter(Object message) throws TransportServiceException
    {
        return createMessageAdapter(message, messageAdapter);
    }

    /* (non-Javadoc)
     * @see org.mule.providers.service.TransportServiceDescriptor#createStreamMessageAdapter(java.io.InputStream, java.io.OutputStream)
     */
//    public UMOStreamMessageAdapter createStreamMessageAdapter(InputStream in, OutputStream out)
//    throws TransportServiceException
//    {
//        if (streamMessageAdapter == null)
//        {
//
//            // If the stream.message.adapter is not set streaming should not be used
//            throw new TransportServiceException(new Message(Messages.X_NOT_SET_IN_SERVICE_X,
//                "stream.message.adapter", service + " service descriptor"));
//        }
//        try
//        {
//            if (out == null)
//            {
//                return (UMOStreamMessageAdapter)ClassUtils.instanciateClass(streamMessageAdapter,
//                    new Object[]{in});
//            }
//            else
//            {
//                return (UMOStreamMessageAdapter)ClassUtils.instanciateClass(streamMessageAdapter,
//                    new Object[]{in, out});
//            }
//        }
//        catch (Exception e)
//        {
//            throw new TransportServiceException(new Message(Messages.FAILED_TO_CREATE_X_WITH_X,
//                "Message Adapter", streamMessageAdapter), e);
//        }
//    }

    protected UMOMessageAdapter createMessageAdapter(Object message, String clazz)
            throws TransportServiceException
    {
        if (message == null)
        {
            message = NullPayload.getInstance();
        }
        if (messageAdapter != null)
        {
            try
            {
                return (UMOMessageAdapter) ClassUtils.instanciateClass(clazz, new Object[]{message});
            }
            catch (Exception e)
            {
                throw new TransportServiceException(CoreMessages.failedToCreateObjectWith("Message Adapter", clazz), e);
            }
        }
        else
        {
            throw new TransportServiceException(CoreMessages.objectNotSetInService("Message Adapter", getService()));
        }
    }

    /* (non-Javadoc)
     * @see org.mule.providers.service.TransportServiceDescriptor#createSessionHandler()
     */
    public UMOSessionHandler createSessionHandler() throws TransportServiceException
    {
        if (sessionHandler == null)
        {
            sessionHandler = MuleSessionHandler.class.getName();
            if (logger.isDebugEnabled())
            {
                logger.debug("No session.handler set in service description, defaulting to: "
                        + sessionHandler);
            }
        }
        try
        {
            return (UMOSessionHandler) ClassUtils.instanciateClass(sessionHandler, ClassUtils.NO_ARGS,
                    getClass());
        }
        catch (Throwable e)
        {
            throw new TransportServiceException(CoreMessages.failedToCreateObjectWith("SessionHandler", sessionHandler), e);
        }
    }

    /* (non-Javadoc)
     * @see org.mule.providers.service.TransportServiceDescriptor#createMessageReceiver(org.mule.umo.provider.UMOConnector, org.mule.umo.UMOComponent, org.mule.umo.endpoint.UMOEndpoint)
     */
    public UMOMessageReceiver createMessageReceiver(UMOConnector connector,
                                                    UMOComponent component,
                                                    UMOImmutableEndpoint endpoint) throws UMOException
    {

        return createMessageReceiver(connector, component, endpoint, null);
    }

    /* (non-Javadoc)
     * @see org.mule.providers.service.TransportServiceDescriptor#createMessageReceiver(org.mule.umo.provider.UMOConnector, org.mule.umo.UMOComponent, org.mule.umo.endpoint.UMOEndpoint, java.lang.Object[])
     */
    public UMOMessageReceiver createMessageReceiver(UMOConnector connector,
                                                    UMOComponent component,
                                                    UMOImmutableEndpoint endpoint,
                                                    Object[] args) throws UMOException
    {
        String receiverClass = messageReceiver;

        if (endpoint.getTransactionConfig() != null
                && endpoint.getTransactionConfig().getAction() != UMOTransactionConfig.ACTION_NONE)
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
            newArgs[1] = component;
            newArgs[2] = endpoint;

            if (args != null && args.length != 0)
            {
                System.arraycopy(args, 0, newArgs, 3, newArgs.length - 3);
            }

            try
            {
                return (UMOMessageReceiver) ClassUtils.instanciateClass(receiverClass, newArgs);
            }
            catch (Exception e)
            {
                throw new TransportServiceException(CoreMessages.failedToCreateObjectWith("Message Receiver", getService()), e);
            }

        }
        else
        {
            throw new TransportServiceException(CoreMessages.objectNotSetInService("Message Receiver", getService()));
        }
    }

    /* (non-Javadoc)
     * @see org.mule.providers.service.TransportServiceDescriptor#createDispatcherFactory()
     */
    public UMOMessageDispatcherFactory createDispatcherFactory() throws TransportServiceException
    {
        if (dispatcherFactory != null)
        {
            try
            {
                return (UMOMessageDispatcherFactory) ClassUtils.instanciateClass(dispatcherFactory,
                        ClassUtils.NO_ARGS);
            }
            catch (Exception e)
            {
                throw new TransportServiceException(CoreMessages.failedToCreateObjectWith("Message Dispatcher Factory", dispatcherFactory), e);
            }
        }
        else
        {
            //Its valide not to have a Dispatcher factory on the transport
            return null;
//            throw new TransportServiceException(new Message(Messages.X_NOT_SET_IN_SERVICE_X,
//                "Message Dispatcher Factory", getService()));
        }
    }

    /* (non-Javadoc)
     * @see org.mule.providers.service.TransportServiceDescriptor#createTransactionFactory()
     */
    public UMOTransactionFactory createTransactionFactory() throws TransportServiceException
    {
        if (transactionFactory != null)
        {
            try
            {
                return (UMOTransactionFactory) ClassUtils.instanciateClass(transactionFactory,
                        ClassUtils.NO_ARGS);
            }
            catch (Exception e)
            {
                throw new TransportServiceException(CoreMessages.failedToCreateObjectWith("Transaction Factory", transactionFactory), e);
            }
        }
        else
        {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see org.mule.providers.service.TransportServiceDescriptor#createConnector(java.lang.String)
     */
    public UMOConnector createConnector() throws TransportServiceException
    {
        UMOConnector newConnector;
        // if there is a factory, use it
        try
        {
            if (connectorFactory != null)
            {
                ObjectFactory factory = (ObjectFactory) ClassUtils.loadClass(connectorFactory,
                        TransportFactory.class).newInstance();
                newConnector = (UMOConnector) factory.getOrCreate();
            }
            else
            {
                if (connector != null)
                {
                    newConnector = (UMOConnector) ClassUtils.loadClass(connector, TransportFactory.class)
                            .newInstance();
                }
                else
                {
                    throw new TransportServiceException(CoreMessages.objectNotSetInService("Connector", getService()));
                }
            }
        }
        catch (TransportServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new TransportServiceException(CoreMessages.failedToCreateObjectWith("Connector", connector), e);

        }

        if (newConnector.getName() == null)
        {
            newConnector.setName("_" + newConnector.getProtocol() + "Connector#" + connector.hashCode());
        }
        return newConnector;
    }

    /* (non-Javadoc)
     * @see org.mule.providers.service.TransportServiceDescriptor#createInboundTransformer()
     */
    public List createInboundTransformers() throws TransportFactoryException
    {
        if (inboundTransformer != null)
        {
            return CollectionUtils.singletonList(inboundTransformer);
        }
        if (defaultInboundTransformer != null)
        {
            logger.info("Loading default inbound transformer: " + defaultInboundTransformer);
            try
            {
                inboundTransformer = (UMOTransformer) ClassUtils.instanciateClass(
                        defaultInboundTransformer, ClassUtils.NO_ARGS);
                return CollectionUtils.singletonList(inboundTransformer);
            }
            catch (Exception e)
            {
                throw new TransportFactoryException(CoreMessages.failedToLoadTransformer("inbound", defaultInboundTransformer), e);
            }
        }
        return TransformerUtils.UNDEFINED;
    }

    /* (non-Javadoc)
     * @see org.mule.providers.service.TransportServiceDescriptor#createOutboundTransformer()
     */
    public List createOutboundTransformers() throws TransportFactoryException
    {
        if (outboundTransformer != null)
        {
            return CollectionUtils.singletonList(outboundTransformer);
        }
        if (defaultOutboundTransformer != null)
        {
            logger.info("Loading default outbound transformer: " + defaultOutboundTransformer);
            try
            {
                outboundTransformer = (UMOTransformer) ClassUtils.instanciateClass(
                        defaultOutboundTransformer, ClassUtils.NO_ARGS);
                return CollectionUtils.singletonList(outboundTransformer);
            }
            catch (Exception e)
            {
                throw new TransportFactoryException(CoreMessages.failedToLoadTransformer("outbound", defaultOutboundTransformer), e);
            }
        }
        return TransformerUtils.UNDEFINED;
    }

    /* (non-Javadoc)
     * @see org.mule.providers.service.TransportServiceDescriptor#createResponseTransformer()
     */
    public List createResponseTransformers() throws TransportFactoryException
    {
        if (responseTransformer != null)
        {
            return CollectionUtils.singletonList(responseTransformer);
        }
        if (defaultResponseTransformer != null)
        {
            logger.info("Loading default response transformer: " + defaultResponseTransformer);
            try
            {
                responseTransformer = (UMOTransformer) ClassUtils.instanciateClass(
                        defaultResponseTransformer, ClassUtils.NO_ARGS);
                return CollectionUtils.singletonList(responseTransformer);
            }
            catch (Exception e)
            {
                throw new TransportFactoryException(CoreMessages.failedToLoadTransformer("response", defaultResponseTransformer), e);
            }
        }
        return TransformerUtils.UNDEFINED;
    }

    /* (non-Javadoc)
     * @see org.mule.providers.service.TransportServiceDescriptor#createEndpointBuilder()
     */
    public EndpointURIBuilder createEndpointBuilder() throws TransportFactoryException
    {
        if (endpointBuilder == null)
        {
            logger.debug("Endpoint resolver not set, Loading default resolver: "
                    + UrlEndpointURIBuilder.class.getName());
            return new UrlEndpointURIBuilder();
        }
        else
        {
            logger.debug("Loading endpointUri resolver: " + endpointBuilder);
            try
            {
                return (EndpointURIBuilder) ClassUtils.instanciateClass(endpointBuilder, ClassUtils.NO_ARGS);
            }
            catch (Exception e)
            {
                throw new TransportFactoryException(CoreMessages.failedToLoad("Endpoint Builder: " + endpointBuilder), e);
            }
        }
    }

    public void setExceptionMappings(Properties props)
    {
        this.exceptionMappings = props;
    }

    public Properties getExceptionMappings()
    {
        return this.exceptionMappings;
    }
}
