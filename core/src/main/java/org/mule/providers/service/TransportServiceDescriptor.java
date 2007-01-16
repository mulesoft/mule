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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>TransportServiceDescriptor</code> describes the necessery information for
 * creating a connector from a service descriptor. A service descriptor should be
 * located at META-INF/services/org/mule/providers/<protocol> where protocol is the
 * protocol of the connector to be created The service descriptor is in the form of
 * string key value pairs and supports a number of properties, descriptions of which
 * can be found here: http://www.muledocs.org/Transport+Service+Descriptors.
 * 
 */

public class TransportServiceDescriptor
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(TransportServiceDescriptor.class);

    private String protocol;
    private String serviceLocation;
    private String serviceError;
    private String serviceFinder;
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
    private Properties properties;

    private UMOTransformer inboundTransformer;
    private UMOTransformer outboundTransformer;
    private UMOTransformer responseTransformer;
    // private EndpointBuilder endpointBuilderImpl;
    private TransportServiceFinder transportServiceFinder;

    public TransportServiceDescriptor(String protocol, String serviceLocation, Properties props)
    {
        this.protocol = protocol;
        this.serviceLocation = serviceLocation;
        this.properties = props;

        serviceError = removeProperty(MuleProperties.CONNECTOR_SERVICE_ERROR);
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
        serviceFinder = removeProperty(MuleProperties.CONNECTOR_SERVICE_FINDER);
        sessionHandler = removeProperty(MuleProperties.CONNECTOR_SESSION_HANDLER);
    }

    void setOverrides(Properties props)
    {
        if (props == null || props.size() == 0)
        {
            return;
        }
        serviceError = props.getProperty(MuleProperties.CONNECTOR_SERVICE_ERROR, serviceError);
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

        temp = props.getProperty(MuleProperties.CONNECTOR_SERVICE_FINDER);
        if (temp != null)
        {
            serviceFinder = temp;
            transportServiceFinder = null;
        }
    }

    private String removeProperty(String name)
    {
        String temp = (String)properties.remove(name);
        if (StringUtils.isEmpty(StringUtils.trim(temp)))
        {
            return null;
        }
        else
        {
            return temp;
        }
    }

    public String getProtocol()
    {
        return protocol;
    }

    public String getServiceLocation()
    {
        return serviceLocation;
    }

    public String getServiceError()
    {
        return serviceError;
    }

    public String getConnector()
    {
        return connector;
    }

    public String getConnectorFactory()
    {
        return connectorFactory;
    }

    public String getDispatcherFactory()
    {
        return dispatcherFactory;
    }

    public String getMessageReceiver()
    {
        return messageReceiver;
    }

    public String getTransactedMessageReceiver()
    {
        return transactedMessageReceiver;
    }

    public String getDefaultInboundTransformer()
    {
        return defaultInboundTransformer;
    }

    public String getDefaultOutboundTransformer()
    {
        return defaultOutboundTransformer;
    }

    public String getMessageAdapter()
    {
        return messageAdapter;
    }

    public Properties getProperties()
    {
        return properties;
    }

    public String getEndpointBuilder()
    {
        return endpointBuilder;
    }

    public String getServiceFinder()
    {
        return serviceFinder;
    }

    public String getStreamMessageAdapter()
    {
        return streamMessageAdapter;
    }

    public String getTransactionFactory()
    {
        return transactionFactory;
    }

    public TransportServiceFinder getConnectorServiceFinder()
    {
        return transportServiceFinder;
    }

    public String getSessionHandler()
    {
        return sessionHandler;
    }

    public TransportServiceFinder createServiceFinder() throws TransportServiceException
    {
        if (serviceFinder == null)
        {
            return null;
        }
        if (transportServiceFinder == null)
        {
            try
            {
                transportServiceFinder = (TransportServiceFinder)ClassUtils.instanciateClass(serviceFinder,
                    ClassUtils.NO_ARGS);
            }
            catch (Exception e)
            {
                throw new TransportServiceException(new Message(Messages.CANT_INSTANCIATE_FINDER_X,
                    serviceFinder), e);
            }
        }
        return transportServiceFinder;
    }

    public String getDefaultResponseTransformer()
    {
        return defaultResponseTransformer;
    }

    public UMOMessageAdapter createMessageAdapter(Object message) throws TransportServiceException
    {
        return createMessageAdapter(message, messageAdapter);
    }

    public UMOStreamMessageAdapter createStreamMessageAdapter(InputStream in, OutputStream out)
        throws TransportServiceException
    {
        if (getStreamMessageAdapter() == null)
        {
            // streamMessageAdapter = StreamMessageAdapter.class.getName();
            if (logger.isDebugEnabled())
            {
                logger.debug("No stream.message.adapter set in service description, defaulting to: "
                             + streamMessageAdapter);
            }
            // If the stream.message.adapter is not set streaming should not be used
            throw new TransportServiceException(new Message(Messages.X_NOT_SET_IN_SERVICE_X,
                "stream.message.adapter", getProtocol() + " service descriptor"));
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
            message = new NullPayload();
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
                "Message Adapter", getProtocol()));
        }
    }

    public UMOSessionHandler createSessionHandler() throws TransportServiceException
    {
        if (getSessionHandler() == null)
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
            return (UMOSessionHandler)ClassUtils.instanciateClass(getSessionHandler(), ClassUtils.NO_ARGS,
                getClass());
        }
        catch (Throwable e)
        {
            throw new TransportServiceException(new Message(Messages.FAILED_TO_CREATE_X_WITH_X,
                "SessionHandler", sessionHandler), e);
        }
    }

    public UMOMessageReceiver createMessageReceiver(UMOConnector connector,
                                                    UMOComponent component,
                                                    UMOEndpoint endpoint) throws UMOException
    {

        return createMessageReceiver(connector, component, endpoint, null);
    }

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
                    "Message Receiver", getProtocol()), e);
            }

        }
        else
        {
            throw new TransportServiceException(new Message(Messages.X_NOT_SET_IN_SERVICE_X,
                "Message Receiver", getProtocol()));
        }
    }

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
            throw new TransportServiceException(new Message(Messages.X_NOT_SET_IN_SERVICE_X,
                "Message Dispatcher Factory", getProtocol()));
        }
    }

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

    public UMOConnector createConnector(String protocol) throws TransportServiceException
    {

        UMOConnector connector;
        // Make sure we can create the endpoint/connector using this service
        // method
        if (getServiceError() != null)
        {
            throw new TransportServiceException(Message.createStaticMessage(getServiceError()));
        }
        // if there is a factory, use it
        try
        {
            if (getConnectorFactory() != null)
            {
                ObjectFactory factory = (ObjectFactory)ClassUtils.loadClass(getConnectorFactory(),
                    TransportFactory.class).newInstance();
                connector = (UMOConnector)factory.create();
            }
            else
            {
                if (getConnector() != null)
                {
                    connector = (UMOConnector)ClassUtils.loadClass(getConnector(), TransportFactory.class)
                        .newInstance();
                }
                else
                {
                    throw new TransportServiceException(new Message(Messages.X_NOT_SET_IN_SERVICE_X,
                        "Connector", getProtocol()));
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
                getConnector()), e);

        }

        if (connector.getName() == null)
        {
            connector.setName("_" + protocol + "Connector#" + connector.hashCode());
        }
        return connector;
    }

    public UMOTransformer createInboundTransformer() throws TransportFactoryException
    {
        if (inboundTransformer != null)
        {
            return inboundTransformer;
        }
        if (getDefaultInboundTransformer() != null)
        {
            logger.info("Loading default inbound transformer: " + getDefaultInboundTransformer());
            try
            {
                inboundTransformer = (UMOTransformer)ClassUtils.instanciateClass(
                    getDefaultInboundTransformer(), ClassUtils.NO_ARGS);
                return inboundTransformer;
            }
            catch (Exception e)
            {
                throw new TransportFactoryException(new Message(Messages.FAILED_LOAD_X_TRANSFORMER_X,
                    "inbound", getDefaultInboundTransformer()), e);
            }
        }
        return null;
    }

    public UMOTransformer createOutboundTransformer() throws TransportFactoryException
    {
        if (outboundTransformer != null)
        {
            return outboundTransformer;
        }
        if (getDefaultOutboundTransformer() != null)
        {
            logger.info("Loading default outbound transformer: " + getDefaultOutboundTransformer());
            try
            {
                outboundTransformer = (UMOTransformer)ClassUtils.instanciateClass(
                    getDefaultOutboundTransformer(), ClassUtils.NO_ARGS);
                return outboundTransformer;
            }
            catch (Exception e)
            {
                throw new TransportFactoryException(new Message(Messages.FAILED_LOAD_X_TRANSFORMER_X,
                    "outbound", getDefaultOutboundTransformer()), e);
            }
        }
        return null;
    }

    public UMOTransformer createResponseTransformer() throws TransportFactoryException
    {
        if (responseTransformer != null)
        {
            return responseTransformer;
        }
        if (getDefaultResponseTransformer() != null)
        {
            logger.info("Loading default response transformer: " + getDefaultResponseTransformer());
            try
            {
                responseTransformer = (UMOTransformer)ClassUtils.instanciateClass(
                    getDefaultResponseTransformer(), ClassUtils.NO_ARGS);
                return responseTransformer;
            }
            catch (Exception e)
            {
                throw new TransportFactoryException(new Message(Messages.FAILED_LOAD_X_TRANSFORMER_X,
                    "response", getDefaultResponseTransformer()), e);
            }
        }
        return null;
    }

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
            logger.debug("Loading endpointUri resolver: " + getEndpointBuilder());
            try
            {
                return (EndpointBuilder)ClassUtils.instanciateClass(getEndpointBuilder(), ClassUtils.NO_ARGS);
            }
            catch (Exception e)
            {
                throw new TransportFactoryException(new Message(Messages.FAILED_LOAD_X,
                    "Endpoint Builder: " + getEndpointBuilder()), e);
            }
        }
    }

    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof TransportServiceDescriptor))
        {
            return false;
        }

        final TransportServiceDescriptor transportServiceDescriptor = (TransportServiceDescriptor)o;

        if (connector != null
                        ? !connector.equals(transportServiceDescriptor.connector)
                        : transportServiceDescriptor.connector != null)
        {
            return false;
        }
        if (connectorFactory != null
                        ? !connectorFactory.equals(transportServiceDescriptor.connectorFactory)
                        : transportServiceDescriptor.connectorFactory != null)
        {
            return false;
        }
        if (defaultInboundTransformer != null
                        ? !defaultInboundTransformer.equals(transportServiceDescriptor.defaultInboundTransformer)
                        : transportServiceDescriptor.defaultInboundTransformer != null)
        {
            return false;
        }
        if (defaultOutboundTransformer != null
                        ? !defaultOutboundTransformer.equals(transportServiceDescriptor.defaultOutboundTransformer)
                        : transportServiceDescriptor.defaultOutboundTransformer != null)
        {
            return false;
        }
        if (defaultResponseTransformer != null
                        ? !defaultResponseTransformer.equals(transportServiceDescriptor.defaultResponseTransformer)
                        : transportServiceDescriptor.defaultResponseTransformer != null)
        {
            return false;
        }
        if (dispatcherFactory != null
                        ? !dispatcherFactory.equals(transportServiceDescriptor.dispatcherFactory)
                        : transportServiceDescriptor.dispatcherFactory != null)
        {
            return false;
        }
        if (endpointBuilder != null
                        ? !endpointBuilder.equals(transportServiceDescriptor.endpointBuilder)
                        : transportServiceDescriptor.endpointBuilder != null)
        {
            return false;
        }
        if (messageAdapter != null
                        ? !messageAdapter.equals(transportServiceDescriptor.messageAdapter)
                        : transportServiceDescriptor.messageAdapter != null)
        {
            return false;
        }
        if (messageReceiver != null
                        ? !messageReceiver.equals(transportServiceDescriptor.messageReceiver)
                        : transportServiceDescriptor.messageReceiver != null)
        {
            return false;
        }
        if (properties != null
                        ? !properties.equals(transportServiceDescriptor.properties)
                        : transportServiceDescriptor.properties != null)
        {
            return false;
        }
        if (protocol != null
                        ? !protocol.equals(transportServiceDescriptor.protocol)
                        : transportServiceDescriptor.protocol != null)
        {
            return false;
        }
        if (serviceError != null
                        ? !serviceError.equals(transportServiceDescriptor.serviceError)
                        : transportServiceDescriptor.serviceError != null)
        {
            return false;
        }
        if (serviceFinder != null
                        ? !serviceFinder.equals(transportServiceDescriptor.serviceFinder)
                        : transportServiceDescriptor.serviceFinder != null)
        {
            return false;
        }
        if (serviceLocation != null
                        ? !serviceLocation.equals(transportServiceDescriptor.serviceLocation)
                        : transportServiceDescriptor.serviceLocation != null)
        {
            return false;
        }
        if (sessionHandler != null
                        ? !sessionHandler.equals(transportServiceDescriptor.sessionHandler)
                        : transportServiceDescriptor.sessionHandler != null)
        {
            return false;
        }
        if (streamMessageAdapter != null
                        ? !streamMessageAdapter.equals(transportServiceDescriptor.streamMessageAdapter)
                        : transportServiceDescriptor.streamMessageAdapter != null)
        {
            return false;
        }
        if (transactedMessageReceiver != null
                        ? !transactedMessageReceiver.equals(transportServiceDescriptor.transactedMessageReceiver)
                        : transportServiceDescriptor.transactedMessageReceiver != null)
        {
            return false;
        }
        if (transactionFactory != null
                        ? !transactionFactory.equals(transportServiceDescriptor.transactionFactory)
                        : transportServiceDescriptor.transactionFactory != null)
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        int result = (protocol != null ? protocol.hashCode() : 0);
        result = 29 * result + (serviceLocation != null ? serviceLocation.hashCode() : 0);
        result = 29 * result + (serviceError != null ? serviceError.hashCode() : 0);
        result = 29 * result + (serviceFinder != null ? serviceFinder.hashCode() : 0);
        result = 29 * result + (connector != null ? connector.hashCode() : 0);
        result = 29 * result + (connectorFactory != null ? connectorFactory.hashCode() : 0);
        result = 29 * result + (dispatcherFactory != null ? dispatcherFactory.hashCode() : 0);
        result = 29 * result + (transactionFactory != null ? transactionFactory.hashCode() : 0);
        result = 29 * result + (messageAdapter != null ? messageAdapter.hashCode() : 0);
        result = 29 * result + (streamMessageAdapter != null ? streamMessageAdapter.hashCode() : 0);
        result = 29 * result + (messageReceiver != null ? messageReceiver.hashCode() : 0);
        result = 29 * result + (transactedMessageReceiver != null ? transactedMessageReceiver.hashCode() : 0);
        result = 29 * result + (endpointBuilder != null ? endpointBuilder.hashCode() : 0);
        result = 29 * result + (sessionHandler != null ? sessionHandler.hashCode() : 0);
        result = 29 * result + (defaultInboundTransformer != null ? defaultInboundTransformer.hashCode() : 0);
        result = 29 * result
                 + (defaultOutboundTransformer != null ? defaultOutboundTransformer.hashCode() : 0);
        result = 29 * result
                 + (defaultResponseTransformer != null ? defaultResponseTransformer.hashCode() : 0);
        return 29 * result + (properties != null ? properties.hashCode() : 0);
    }

}
