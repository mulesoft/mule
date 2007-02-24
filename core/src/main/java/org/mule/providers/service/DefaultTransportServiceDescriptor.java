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
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleSessionHandler;
import org.mule.impl.endpoint.EndpointBuilder;
import org.mule.impl.endpoint.UrlEndpointBuilder;
import org.mule.providers.NullPayload;
import org.mule.registry.AbstractServiceDescriptor;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.UMOTransactionFactory;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.provider.UMOMessageDispatcherFactory;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.umo.provider.UMOSessionHandler;
import org.mule.umo.provider.UMOStreamMessageAdapter;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.ClassUtils;
import org.mule.util.ObjectFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * @inheritDocs
 */
public class DefaultTransportServiceDescriptor extends AbstractServiceDescriptor implements TransportServiceDescriptor
{
    private String connector;
    private String connectorFactory;
    private String dispatcherFactory;
    private String transactionFactory;
    private String messageAdapter;
    private String streamMessageAdapter;
    private String messageReceiver;
    private String transactedMessageReceiver;
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

    public DefaultTransportServiceDescriptor(String service, Properties props)
    {
        super(service, props);

        connector = removeProperty(MuleProperties.CONNECTOR_CLASS);
        connectorFactory = removeProperty(MuleProperties.CONNECTOR_FACTORY);
        dispatcherFactory = removeProperty(MuleProperties.CONNECTOR_DISPATCHER_FACTORY);
        transactionFactory = removeProperty(MuleProperties.CONNECTOR_DISPATCHER_FACTORY);
        messageReceiver = removeProperty(MuleProperties.CONNECTOR_MESSAGE_RECEIVER_CLASS);
        transactedMessageReceiver = removeProperty(MuleProperties.CONNECTOR_TRANSACTED_MESSAGE_RECEIVER_CLASS);
        messageAdapter = removeProperty(MuleProperties.CONNECTOR_MESSAGE_ADAPTER);
        streamMessageAdapter = removeProperty(MuleProperties.CONNECTOR_STREAM_MESSAGE_ADAPTER);
        defaultInboundTransformer = removeProperty(MuleProperties.CONNECTOR_INBOUND_TRANSFORMER);
        defaultOutboundTransformer = removeProperty(MuleProperties.CONNECTOR_OUTBOUND_TRANSFORMER);
        defaultResponseTransformer = removeProperty(MuleProperties.CONNECTOR_RESPONSE_TRANSFORMER);
        endpointBuilder = removeProperty(MuleProperties.CONNECTOR_ENDPOINT_BUILDER);
        sessionHandler = removeProperty(MuleProperties.CONNECTOR_SESSION_HANDLER);
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
    public UMOStreamMessageAdapter createStreamMessageAdapter(InputStream in, OutputStream out)
    throws TransportServiceException
    {
        if (streamMessageAdapter == null)
        {
    
            // If the stream.message.adapter is not set streaming should not be used
            throw new TransportServiceException(new Message(Messages.X_NOT_SET_IN_SERVICE_X,
                "stream.message.adapter", service + " service descriptor"));
        }
        try
        {
            if (out == null)
            {
                return (UMOStreamMessageAdapter)ClassUtils.instanciateClass(streamMessageAdapter,
                    new Object[]{in});
            }
            else
            {
                return (UMOStreamMessageAdapter)ClassUtils.instanciateClass(streamMessageAdapter,
                    new Object[]{in, out});
            }
        }
        catch (Exception e)
        {
            throw new TransportServiceException(new Message(Messages.FAILED_TO_CREATE_X_WITH_X,
                "Message Adapter", streamMessageAdapter), e);
        }
    }

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
                return (UMOMessageAdapter)ClassUtils.instanciateClass(clazz, new Object[]{message});
            }
            catch (Exception e)
            {
                throw new TransportServiceException(new Message(Messages.FAILED_TO_CREATE_X_WITH_X,
                    "Message Adapter", clazz), e);
            }
        }
        else
        {
            throw new TransportServiceException(new Message(Messages.X_NOT_SET_IN_SERVICE_X,
                "Message Adapter", getService()));
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
            return (UMOSessionHandler)ClassUtils.instanciateClass(sessionHandler, ClassUtils.NO_ARGS,
                getClass());
        }
        catch (Throwable e)
        {
            throw new TransportServiceException(new Message(Messages.FAILED_TO_CREATE_X_WITH_X,
                "SessionHandler", sessionHandler), e);
        }
    }

    /* (non-Javadoc)
     * @see org.mule.providers.service.TransportServiceDescriptor#createMessageReceiver(org.mule.umo.provider.UMOConnector, org.mule.umo.UMOComponent, org.mule.umo.endpoint.UMOEndpoint)
     */
    public UMOMessageReceiver createMessageReceiver(UMOConnector connector,
                                                    UMOComponent component,
                                                    UMOEndpoint endpoint) throws UMOException
    {

        return createMessageReceiver(connector, component, endpoint, null);
    }

    /* (non-Javadoc)
     * @see org.mule.providers.service.TransportServiceDescriptor#createMessageReceiver(org.mule.umo.provider.UMOConnector, org.mule.umo.UMOComponent, org.mule.umo.endpoint.UMOEndpoint, java.lang.Object[])
     */
    public UMOMessageReceiver createMessageReceiver(UMOConnector connector,
                                                    UMOComponent component,
                                                    UMOEndpoint endpoint,
                                                    Object[] args) throws UMOException
    {
        String receiverClass = messageReceiver;

        if (endpoint.getTransactionConfig() != null
            && endpoint.getTransactionConfig().getAction() != UMOTransactionConfig.ACTION_NONE)
        {
            if (transactedMessageReceiver != null)
            {
                receiverClass = transactedMessageReceiver;
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
                return (UMOMessageReceiver)ClassUtils.instanciateClass(receiverClass, newArgs);
            }
            catch (Exception e)
            {
                throw new TransportServiceException(new Message(Messages.FAILED_TO_CREATE_X_WITH_X,
                    "Message Receiver", getService()), e);
            }

        }
        else
        {
            throw new TransportServiceException(new Message(Messages.X_NOT_SET_IN_SERVICE_X,
                "Message Receiver", getService()));
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
                return (UMOMessageDispatcherFactory)ClassUtils.instanciateClass(dispatcherFactory,
                    ClassUtils.NO_ARGS);
            }
            catch (Exception e)
            {
                throw new TransportServiceException(new Message(Messages.FAILED_TO_CREATE_X_WITH_X,
                    "Message Dispatcher Factory", dispatcherFactory), e);
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
                return (UMOTransactionFactory)ClassUtils.instanciateClass(transactionFactory,
                    ClassUtils.NO_ARGS);
            }
            catch (Exception e)
            {
                throw new TransportServiceException(new Message(Messages.FAILED_TO_CREATE_X_WITH_X,
                    "Transaction Factory", transactionFactory), e);
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
                ObjectFactory factory = (ObjectFactory)ClassUtils.loadClass(connectorFactory,
                    TransportFactory.class).newInstance();
                newConnector = (UMOConnector)factory.create();
            }
            else
            {
                if (connector != null)
                {
                    newConnector = (UMOConnector)ClassUtils.loadClass(connector, TransportFactory.class)
                        .newInstance();
                }
                else
                {
                    throw new TransportServiceException(new Message(Messages.X_NOT_SET_IN_SERVICE_X,
                        "Connector", getService()));
                }
            }
        }
        catch (TransportServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new TransportServiceException(new Message(Messages.FAILED_TO_CREATE_X_WITH_X, "Connector",
                connector), e);

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
    public UMOTransformer createInboundTransformer() throws TransportFactoryException
    {
        if (inboundTransformer != null)
        {
            return inboundTransformer;
        }
        if (defaultInboundTransformer != null)
        {
            logger.info("Loading default inbound transformer: " + defaultInboundTransformer);
            try
            {
                inboundTransformer = (UMOTransformer)ClassUtils.instanciateClass(
                    defaultInboundTransformer, ClassUtils.NO_ARGS);
                return inboundTransformer;
            }
            catch (Exception e)
            {
                throw new TransportFactoryException(new Message(Messages.FAILED_LOAD_X_TRANSFORMER_X,
                    "inbound", defaultInboundTransformer), e);
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.mule.providers.service.TransportServiceDescriptor#createOutboundTransformer()
     */
    public UMOTransformer createOutboundTransformer() throws TransportFactoryException
    {
        if (outboundTransformer != null)
        {
            return outboundTransformer;
        }
        if (defaultOutboundTransformer != null)
        {
            logger.info("Loading default outbound transformer: " + defaultOutboundTransformer);
            try
            {
                outboundTransformer = (UMOTransformer)ClassUtils.instanciateClass(
                    defaultOutboundTransformer, ClassUtils.NO_ARGS);
                return outboundTransformer;
            }
            catch (Exception e)
            {
                throw new TransportFactoryException(new Message(Messages.FAILED_LOAD_X_TRANSFORMER_X,
                    "outbound", defaultOutboundTransformer), e);
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.mule.providers.service.TransportServiceDescriptor#createResponseTransformer()
     */
    public UMOTransformer createResponseTransformer() throws TransportFactoryException
    {
        if (responseTransformer != null)
        {
            return responseTransformer;
        }
        if (defaultResponseTransformer != null)
        {
            logger.info("Loading default response transformer: " + defaultResponseTransformer);
            try
            {
                responseTransformer = (UMOTransformer)ClassUtils.instanciateClass(
                    defaultResponseTransformer, ClassUtils.NO_ARGS);
                return responseTransformer;
            }
            catch (Exception e)
            {
                throw new TransportFactoryException(new Message(Messages.FAILED_LOAD_X_TRANSFORMER_X,
                    "response", defaultResponseTransformer), e);
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.mule.providers.service.TransportServiceDescriptor#createEndpointBuilder()
     */
    public EndpointBuilder createEndpointBuilder() throws TransportFactoryException
    {
        if (endpointBuilder == null)
        {
            logger.debug("Endpoint resolver not set, Loading default resolver: "
                         + UrlEndpointBuilder.class.getName());
            return new UrlEndpointBuilder();
        }
        else
        {
            logger.debug("Loading endpointUri resolver: " + endpointBuilder);
            try
            {
                return (EndpointBuilder)ClassUtils.instanciateClass(endpointBuilder, ClassUtils.NO_ARGS);
            }
            catch (Exception e)
            {
                throw new TransportFactoryException(new Message(Messages.FAILED_LOAD_X,
                    "Endpoint Builder: " + endpointBuilder), e);
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
