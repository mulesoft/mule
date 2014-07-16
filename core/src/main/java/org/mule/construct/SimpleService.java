/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.source.MessageSource;
import org.mule.config.i18n.MessageFactory;
import org.mule.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * In-out SOA-style simple service, with no outbound router. Always fully synchronous.
 */
public class SimpleService extends AbstractConfigurationPattern
{
    public enum Type
    {
        /**
         * Expose a JAX-WS annoted component as a web service. The CXF module is required to have this working.
         */
        JAX_WS
        {
            @Override
            public void validate(Component component) throws FlowConstructInvalidException
            {
                if (!(component instanceof JavaComponent))
                {
                    throw new FlowConstructInvalidException(
                        MessageFactory.createStaticMessage("SimpleService can only expose instances of JAX-WS annotated JavaComponent instances. You provided a: "
                                                           + component.getClass().getName()));
                }
            }

            @Override
            public void configureComponentMessageProcessor(MuleContext muleContext,
                                                           MessageProcessorChainBuilder builder,
                                                           Component component)
            {
                builder.chain(newJaxWsComponentMessageProcessor(muleContext, getComponentClass(component)));
                builder.chain(component);
            }
        },

        /**
         * Expose a JAX-RS annoted component as a web service. The Jersey module is required to have this working.
         */
        JAX_RS
        {
            @Override
            public void validate(Component component) throws FlowConstructInvalidException
            {
                if (!(component instanceof JavaComponent))
                {
                    throw new FlowConstructInvalidException(
                        MessageFactory.createStaticMessage("SimpleService can only expose instances of JAX-RS annotated JavaComponent instances. You provided a: "
                                                           + component.getClass().getName()));
                }
            }

            @Override
            public void configureComponentMessageProcessor(MuleContext muleContext,
                                                           MessageProcessorChainBuilder builder,
                                                           Component component)
            {
                builder.chain(newJaxRsComponentWrapper(muleContext, component));
            }
        },

        /**
         * Pass the inbound messages unaltered to the component.
         */
        DIRECT
        {
            @Override
            public void validate(Component component) throws FlowConstructInvalidException
            {
                // NOOP
            }

            @Override
            public void configureComponentMessageProcessor(MuleContext muleContext,
                                                           MessageProcessorChainBuilder builder,
                                                           Component component)
            {
                builder.chain(component);
            }
        };

        public abstract void validate(Component component) throws FlowConstructInvalidException;

        public abstract void configureComponentMessageProcessor(MuleContext muleContext,
                                                                MessageProcessorChainBuilder builder,
                                                                Component component);

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
                         List<MessageProcessor> transformers,
                         List<MessageProcessor> responseTransformers,
                         Component component,
                         Type type) throws MuleException
    {
        super(name, muleContext, transformers, responseTransformers);

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
    protected void configureMessageProcessorsBeforeTransformation(MessageProcessorChainBuilder builder)
    {
        // NOOP
    }

    @Override
    protected void configureMessageProcessorsAfterTransformation(MessageProcessorChainBuilder builder)
    {
        type.configureComponentMessageProcessor(muleContext, builder, component);
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

        type.validate(component);
    }

    private static Class<?> getComponentClass(Component component)
    {
        if (component instanceof JavaComponent)
        {
            return ((JavaComponent) component).getObjectFactory().getObjectClass();
        }

        return component.getClass();
    }

    private static MessageProcessor newJaxWsComponentMessageProcessor(MuleContext muleContext,
                                                                      Class<?> componentClass)
    {
        try
        {
            MessageProcessorBuilder wsmpb = (MessageProcessorBuilder) ClassUtils.instanciateClass("org.mule.module.cxf.builder.WebServiceMessageProcessorBuilder");

            Method setServiceClassMethod = ClassUtils.getMethod(wsmpb.getClass(), "setServiceClass",
                new Class<?>[]{Class.class});

            setServiceClassMethod.invoke(wsmpb, componentClass);

            Method setFrontendMethod = ClassUtils.getMethod(wsmpb.getClass(), "setFrontend",
                new Class<?>[]{String.class});

            setFrontendMethod.invoke(wsmpb, "jaxws");

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

    private static Component newJaxRsComponentWrapper(MuleContext muleContext, Component component)
    {
        try
        {
            Component jrc = (Component) ClassUtils.instanciateClass("org.mule.module.jersey.JerseyResourcesComponent");

            Method setComponentsMethod = ClassUtils.getMethod(jrc.getClass(), "setComponents",
                new Class<?>[]{List.class});

            setComponentsMethod.invoke(jrc, Collections.singletonList(component));

            ((MuleContextAware) jrc).setMuleContext(muleContext);

            return jrc;
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(
                MessageFactory.createStaticMessage("Failed to configure the required web service infrastructure: are you missing the Mule Jersey Module?"),
                e);
        }
    }

    @Override
    public String getConstructType()
    {
        return "Simple-Service";
    }
}
