/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.construct;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.component.Component;
import org.mule.api.component.JavaComponent;
import org.mule.api.construct.FlowConstructInvalidException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.api.source.MessageSource;
import org.mule.config.i18n.MessageFactory;
import org.mule.construct.processor.FlowConstructStatisticsMessageObserver;
import org.mule.interceptor.LoggingInterceptor;
import org.mule.processor.builder.InterceptingChainMessageProcessorBuilder;
import org.mule.util.ClassUtils;

import java.lang.reflect.Method;

/**
 * In-out SOA-style simple service, with no outbound router. Always fully
 * synchronous.
 */
public class SimpleService extends AbstractFlowConstruct
{
    // TODO add support for JAX-RS
    public enum Type
    {
        /**
         * Expose a JAX-WS annoted component as a web service. CXF is required to
         * have this working.
         */
        JAX_WS,
        /**
         * Pass the inbound messages unaltered to the component.
         */
        DEFAULT;

        public static Type fromString(String string)
        {
            String mepString = string.toUpperCase().replace('-', '_');
            return Type.valueOf(mepString);
        }
    }

    private final Component component;
    private final Type type;

    public SimpleService(String name,
                         MuleContext muleContext,
                         MessageSource messageSource,
                         Component component,
                         Type type) throws MuleException
    {
        super(name, muleContext);

        if (messageSource == null)
        {
            throw new FlowConstructInvalidException(
                MessageFactory.createStaticMessage("messageSource can't be null on: " + this.toString()));
        }

        if (component == null)
        {
            throw new FlowConstructInvalidException(
                MessageFactory.createStaticMessage("component can't be null on: " + this.toString()));
        }

        if (type == null)
        {
            throw new FlowConstructInvalidException(
                MessageFactory.createStaticMessage("type can't be null on: " + this.toString()));
        }

        this.messageSource = messageSource;
        this.component = component;
        this.type = type;
    }

    public Component getComponent()
    {
        return component;
    }

    @Override
    protected void configureMessageProcessors(InterceptingChainMessageProcessorBuilder builder)
    {
        builder.chain(new LoggingInterceptor());
        builder.chain(new FlowConstructStatisticsMessageObserver());

        if (type != Type.DEFAULT)
        {
            configureComponentMessageProcessor(builder);
        }

        builder.chain(component);
    }

    @Override
    protected void validateConstruct() throws FlowConstructInvalidException
    {
        super.validateConstruct();

        if ((messageSource instanceof InboundEndpoint)
            && (!((InboundEndpoint) messageSource).getExchangePattern().equals(
                MessageExchangePattern.REQUEST_RESPONSE)))
        {
            throw new FlowConstructInvalidException(
                MessageFactory.createStaticMessage("SimpleService only works with a request-response inbound endpoint."),
                this);
        }

        if ((type == Type.JAX_WS) && (!(component instanceof JavaComponent)))
        {
            throw new FlowConstructInvalidException(
                MessageFactory.createStaticMessage("SimpleService can only expose instances of JavaComponent as web services."),
                this);
        }
    }

    private void configureComponentMessageProcessor(InterceptingChainMessageProcessorBuilder builder)
    {
        Class<?> componentClass = getComponentClass();

        if (type == Type.JAX_WS)
        {
            builder.chain(newWebServiceMessageProcessor(componentClass));
        }
    }

    private Class<?> getComponentClass()
    {
        if (component instanceof JavaComponent)
        {
            return ((JavaComponent) component).getObjectFactory().getObjectClass();
        }

        return component.getClass();
    }

    private MessageProcessor newWebServiceMessageProcessor(Class<?> componentClass)
    {
        try
        {
            MessageProcessorBuilder wsmpb = (MessageProcessorBuilder) ClassUtils.instanciateClass("org.mule.module.cxf.builder.WebServiceMessageProcessorBuilder");

            Method setServiceClassMethod = ClassUtils.getMethod(wsmpb.getClass(), "setServiceClass",
                new Class<?>[]{Class.class});

            setServiceClassMethod.invoke(wsmpb, new Object[]{componentClass});

            Method setFrontendMethod = ClassUtils.getMethod(wsmpb.getClass(), "setFrontend",
                new Class<?>[]{String.class});

            setFrontendMethod.invoke(wsmpb, new Object[]{"jaxws"});

            ((MuleContextAware) wsmpb).setMuleContext(muleContext);

            return wsmpb.build();
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(
                MessageFactory.createStaticMessage("Failed to configure the required web service infrastructure: are you missing the Mule CXF Module?"),
                e);
        }
    }

}
