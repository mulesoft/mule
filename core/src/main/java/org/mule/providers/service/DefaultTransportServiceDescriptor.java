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
import org.mule.impl.endpoint.EndpointBuilder;
import org.mule.providers.NullPayload;
import org.mule.registry.AbstractServiceDescriptor;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.UMOTransactionFactory;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

/**
 * @inheritDocs
 */
public class DefaultTransportServiceDescriptor extends AbstractServiceDescriptor implements TransportServiceDescriptor, Initialisable
{
    private String connector;
    private String connectorFactory;
    private String dispatcherFactory;
    private String transactionFactory;
    private String messageAdapter;
    private String streamMessageAdapter;
    private String messageReceiver;
    private String transactedMessageReceiver;
    private Properties exceptionMappings = new Properties();
    private Properties servicePorperties;

    StaticApplicationContext context;

    public DefaultTransportServiceDescriptor(String service, Properties props, ApplicationContext appContext) throws ClassNotFoundException
    {
        super(service, props);

        if(appContext!=null)
        {
            context = new StaticApplicationContext(appContext);

            AbstractBeanFactory beanFactory = (AbstractBeanFactory)appContext.getAutowireCapableBeanFactory();
            context.getBeanFactory().copyConfigurationFrom(beanFactory);
        }
        else
        {
            context = new StaticApplicationContext();
        }

        messageReceiver = props.getProperty(MuleProperties.CONNECTOR_MESSAGE_RECEIVER_CLASS);
        messageAdapter = props.getProperty(MuleProperties.CONNECTOR_MESSAGE_ADAPTER);
        streamMessageAdapter = props.getProperty(MuleProperties.CONNECTOR_STREAM_MESSAGE_ADAPTER);

        registerService(MuleProperties.CONNECTOR_CLASS, null, false);
        registerService(MuleProperties.CONNECTOR_FACTORY, null, false);
        registerService(MuleProperties.CONNECTOR_DISPATCHER_FACTORY, null, false);
        registerService(MuleProperties.CONNECTOR_MESSAGE_RECEIVER_CLASS, null, false);
        registerService(MuleProperties.CONNECTOR_TRANSACTED_MESSAGE_RECEIVER_CLASS, null, false);
        registerService(MuleProperties.CONNECTOR_MESSAGE_ADAPTER, null, false);
        registerService(MuleProperties.CONNECTOR_STREAM_MESSAGE_ADAPTER, null, false);
        registerService(MuleProperties.CONNECTOR_INBOUND_TRANSFORMER, null, false);
        registerService(MuleProperties.CONNECTOR_OUTBOUND_TRANSFORMER, null, false);
        registerService(MuleProperties.CONNECTOR_RESPONSE_TRANSFORMER, null, false);
        registerService(MuleProperties.CONNECTOR_ENDPOINT_BUILDER, null, false);
        registerService(MuleProperties.CONNECTOR_SESSION_HANDLER, MuleSessionHandler.class, false);
    }


    public void initialise() throws InitialisationException
    {
        //To change body of implemented methods use File | Settings | File Templates.
        System.out.println("");
    }

    public void setOverrides(Properties props)
    {
        if (props == null || props.size() == 0)
        {
            return;
        }
        this.properties.putAll(props);
//        connector = props.getProperty(MuleProperties.CONNECTOR_CLASS, connector);
//        connectorFactory = props.getProperty(MuleProperties.CONNECTOR_FACTORY, connectorFactory);
//        dispatcherFactory = props.getProperty(MuleProperties.CONNECTOR_DISPATCHER_FACTORY, dispatcherFactory);
//        messageReceiver = props.getProperty(MuleProperties.CONNECTOR_MESSAGE_RECEIVER_CLASS, messageReceiver);
//        transactedMessageReceiver = props.getProperty(
//            MuleProperties.CONNECTOR_TRANSACTED_MESSAGE_RECEIVER_CLASS, transactedMessageReceiver);
//        messageAdapter = props.getProperty(MuleProperties.CONNECTOR_MESSAGE_ADAPTER, messageAdapter);
//
//        String temp = props.getProperty(MuleProperties.CONNECTOR_INBOUND_TRANSFORMER);
//        if (temp != null)
//        {
//            defaultInboundTransformer = temp;
//            inboundTransformer = null;
//        }
//
//        temp = props.getProperty(MuleProperties.CONNECTOR_OUTBOUND_TRANSFORMER);
//        if (temp != null)
//        {
//            defaultOutboundTransformer = temp;
//            outboundTransformer = null;
//        }
//
//        temp = props.getProperty(MuleProperties.CONNECTOR_RESPONSE_TRANSFORMER);
//        if (temp != null)
//        {
//            defaultResponseTransformer = temp;
//            responseTransformer = null;
//        }
//
//        temp = props.getProperty(MuleProperties.CONNECTOR_ENDPOINT_BUILDER);
//        if (temp != null)
//        {
//            endpointBuilder = temp;
//        }
    }


    protected void registerService(String name, Class defaultService, boolean useFactory) throws ClassNotFoundException
    {
        Class serviceClass = removeClassProperty(name);
        if(serviceClass==null)
        {
            if(defaultService!=null)
            {
                serviceClass = defaultService;
                logger.debug("No connector service registered for key: " + name + ". Using default: " + serviceClass);
            }
            else
            {
                logger.debug("No connector service registered for key: " + name + ". No default set either");
            }
        }
//        if(useFactory && serviceClass!=null)
//        {
//
//            context.getBeanFactory().registerScope();
//            factory.setServiceClass(serviceClass);
//        }
        if(serviceClass!=null)
        {
            RootBeanDefinition bd = new RootBeanDefinition(serviceClass, false);
           // bd.setInitMethodName("initialise");
            bd.setDestroyMethodName("dispose");
            bd.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            bd.setEnforceDestroyMethod(false);
            //bd.setEnforceInitMethod(false);
            context.registerBeanDefinition(name, bd);
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
            throw new TransportServiceException(CoreMessages.objectNotSetInService(
                    MuleProperties.CONNECTOR_STREAM_MESSAGE_ADAPTER, service));
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
            throw new TransportServiceException(CoreMessages.failedToCreateObjectWith("Message Adapter", streamMessageAdapter), e);
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
                throw new TransportServiceException(CoreMessages.failedToCreateObjectWith("Message Adapter", clazz), e);
            }
        }
        else
        {
            throw new TransportServiceException(CoreMessages.objectNotSetInService("Message Adapter", getService()));
        }
    }

    protected Object getServiceObject(String name, Class classType) throws TransportServiceException
    {
        Object service;
        try
        {
            if(classType==null)
            {
                service = context.getBean(name);
            }
            else
            {
                service = context.getBean(name, classType);
            }
            return service;
        }
        catch (BeansException e)
        {
            throw new TransportServiceException(CoreMessages.failedToCreateObjectWith(name, classType), e);
        }

    }
    /* (non-Javadoc)
     * @see org.mule.providers.service.TransportServiceDescriptor#createSessionHandler()
     */
    public UMOSessionHandler createSessionHandler() throws TransportServiceException
    {
        return (UMOSessionHandler)getServiceObject(MuleProperties.CONNECTOR_SESSION_HANDLER, UMOSessionHandler.class);
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
                throw new TransportServiceException(CoreMessages.failedToCreateObjectWith("Message Receiver", getService()), e);
            }

        }
        else
        {
            throw new TransportServiceException(CoreMessages.failedToCreateObjectWith("Message Receiver", getService()));
        }
    }

    /* (non-Javadoc)
     * @see org.mule.providers.service.TransportServiceDescriptor#createDispatcherFactory()
     */
    public UMOMessageDispatcherFactory createDispatcherFactory() throws TransportServiceException
    {
        try
        {
            return (UMOMessageDispatcherFactory)context.getBean(MuleProperties.CONNECTOR_DISPATCHER_FACTORY);
        }
        catch (BeansException e)
        {
            logger.debug(e.getMessage());
            return null;
        }
    }

    /* (non-Javadoc)
     * @see org.mule.providers.service.TransportServiceDescriptor#createTransactionFactory()
     */
    public UMOTransactionFactory createTransactionFactory() throws TransportServiceException
    {
        try
        {
            return (UMOTransactionFactory)context.getBean(MuleProperties.CONNECTOR_TRANSACTION_FACTORY);
        }
        catch (BeansException e)
        {
            logger.debug(e.getMessage());
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
            if (context.containsBean(MuleProperties.CONNECTOR_FACTORY))
            {
                ObjectFactory factory = (ObjectFactory)context.getBean(MuleProperties.CONNECTOR_FACTORY);
                newConnector = (UMOConnector)factory.create();
            }
            else
            {
                if (context.containsBean(MuleProperties.CONNECTOR_CLASS))
                {
                    newConnector = (UMOConnector)context.getBean(MuleProperties.CONNECTOR_CLASS);
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
            throw new TransportServiceException(CoreMessages.failedToCreateObjectWith("Connector",
                connector), e);

        }

        if (newConnector.getName() == null)
        {
            newConnector.setName("_" + newConnector.getProtocol() + "Connector#" + newConnector.hashCode());
        }
        return newConnector;
    }

    /* (non-Javadoc)
     * @see org.mule.providers.service.TransportServiceDescriptor#createInboundTransformer()
     */
    public UMOTransformer createInboundTransformer() throws TransportFactoryException
    {
        if (context.containsBean(MuleProperties.CONNECTOR_INBOUND_TRANSFORMER))
        {
            try
            {
                UMOTransformer t = (UMOTransformer)context.getBean(MuleProperties.CONNECTOR_INBOUND_TRANSFORMER);
                logger.info("Loaded default inbound transformer: " + t);
                return t;
            }
            catch (Exception e)
            {
                throw new TransportFactoryException(CoreMessages.failedToLoadTransformer("inbound", MuleProperties.CONNECTOR_INBOUND_TRANSFORMER), e);
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.mule.providers.service.TransportServiceDescriptor#createOutboundTransformer()
     */
    public UMOTransformer createOutboundTransformer() throws TransportFactoryException
    {
        if (context.containsBean(MuleProperties.CONNECTOR_OUTBOUND_TRANSFORMER))
        {
            try
            {
                UMOTransformer t = (UMOTransformer)context.getBean(MuleProperties.CONNECTOR_OUTBOUND_TRANSFORMER);
                logger.info("Loaded default outbound transformer: " + t);
                return t;
            }
            catch (Exception e)
            {
                throw new TransportFactoryException(CoreMessages.failedToLoadTransformer("outbound", MuleProperties.CONNECTOR_OUTBOUND_TRANSFORMER), e);
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.mule.providers.service.TransportServiceDescriptor#createResponseTransformer()
     */
    public UMOTransformer createResponseTransformer() throws TransportFactoryException
    {
        if (context.containsBean(MuleProperties.CONNECTOR_RESPONSE_TRANSFORMER))
        {
            try
            {
                UMOTransformer t = (UMOTransformer)context.getBean(MuleProperties.CONNECTOR_RESPONSE_TRANSFORMER);
                logger.info("Loaded default response transformer: " + t);
                return t;
            }
            catch (Exception e)
            {
                throw new TransportFactoryException(CoreMessages.failedToLoadTransformer("response", MuleProperties.CONNECTOR_RESPONSE_TRANSFORMER), e);
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.mule.providers.service.TransportServiceDescriptor#createEndpointBuilder()
     */
    public EndpointBuilder createEndpointBuilder() throws TransportFactoryException
    {
        EndpointBuilder epb = null;
        if (context.containsBean(MuleProperties.CONNECTOR_ENDPOINT_BUILDER))
        {
            epb = (EndpointBuilder)context.getBean(MuleProperties.CONNECTOR_ENDPOINT_BUILDER);
        }
        if(epb==null)
        {
            throw new TransportFactoryException(CoreMessages.objectNotSetInService(
                    MuleProperties.CONNECTOR_ENDPOINT_BUILDER, getService()));
        }
        return epb;
    }
    
    public void setExceptionMappings(Properties props) 
    {
        this.exceptionMappings = props;
    }
    
    public Properties getExceptionMappings() 
    {
        return this.exceptionMappings;
    }

    private class ServiceFactoryBean implements FactoryBean
    {

        private Object[] constructorArgs;
        private Class serviceClass;


        public Object getObject() throws Exception
        {
            if(getConstructorArgs()==null)
            {
                throw new IllegalStateException("Constructir Args not set");
            }
            Object o = ClassUtils.instanciateClass(serviceClass, constructorArgs);
            setConstructorArgs(null);
            return o;
        }

        public Class getObjectType()
        {
            return UMOMessageAdapter.class;
        }

        public boolean isSingleton()
        {
            return false;
        }


        public Object[] getConstructorArgs()
        {
            return constructorArgs;
        }

        public void setConstructorArgs(Object[] constructorArgs)
        {
            this.constructorArgs = constructorArgs;
        }

        public Class getServiceClass()
        {
            return serviceClass;
        }

        public void setServiceClass(Class serviceClass)
        {
            this.serviceClass = serviceClass;
        }
    }
}
