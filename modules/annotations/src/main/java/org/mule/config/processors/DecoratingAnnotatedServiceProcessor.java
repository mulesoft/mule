/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.processors;

import org.mule.api.AnnotationException;
import org.mule.api.EndpointAnnotationParser;
import org.mule.api.MessageProcessorAnnotationParser;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.annotations.meta.Channel;
import org.mule.api.annotations.meta.ChannelType;
import org.mule.api.annotations.meta.Router;
import org.mule.api.annotations.meta.RouterType;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.registry.PreInitProcessor;
import org.mule.api.routing.OutboundRouter;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.api.source.CompositeMessageSource;
import org.mule.component.AbstractJavaComponent;
import org.mule.config.AnnotationsParserFactory;
import org.mule.config.endpoint.AnnotatedEndpointHelper;
import org.mule.config.i18n.AnnotationsMessages;
import org.mule.registry.RegistryMap;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.util.TemplateParser;
import org.mule.util.annotation.AnnotationMetaData;
import org.mule.util.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This object processor allows users to register annotated services directly to the registry
 * and have them configured correctly.
 * It will look for a non-system {@link org.mule.api.model.Model} registered with the Registry.
 * If one is not found a default  SEDA Model will be created
 * Finally, the processor will register the service with the Registry and return null.
 *
 * @deprecated as of 3.7.0 since these are only used by {@link org.mule.registry.TransientRegistry} which is also deprecated. Use post processors
 * for currently supported registries instead (i.e: {@link org.mule.config.spring.SpringRegistry})
 */
@Deprecated
public class DecoratingAnnotatedServiceProcessor implements PreInitProcessor, MuleContextAware, Startable
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(DecoratingAnnotatedServiceProcessor.class);

    protected MuleContext context;
    private final TemplateParser parser = TemplateParser.createAntStyleParser();
    protected RegistryMap regProps;
    protected AnnotatedEndpointHelper helper;
    protected AnnotationsParserFactory parserFactory;

    public DecoratingAnnotatedServiceProcessor()
    {
    }

    public DecoratingAnnotatedServiceProcessor(MuleContext context)
    {
        setMuleContext(context);
    }

    public void setMuleContext(MuleContext context)
    {
        this.context = context;
        this.regProps = new RegistryMap(context.getRegistry());
    }


    @Override
    public void start() throws MuleException
    {
        helper = new AnnotatedEndpointHelper(context);
        parserFactory = context.getRegistry().lookupObject(AnnotationsParserFactory.class);
        if (parserFactory == null)
        {
            logger.info(AnnotationsParserFactory.class.getName() + " implementation not found in registry, annotations not enabled");
        }
    }

    public Object process(Object object)
    {
        if (object == null || parserFactory == null)
        {
            return object;
        }

        if (object instanceof Service)
        {
            Service service = (Service) object;
            //Annotations only supported on Java components
            if (service.getComponent() instanceof AbstractJavaComponent)
            {
                try
                {
                    AbstractJavaComponent component = (AbstractJavaComponent) service.getComponent();
                    if(AnnotationUtils.getMethodMetaAnnotations(component.getObjectType(), Channel.class).size()==0)
                    {
                        return object;
                    }
                    
                    processInbound(component.getObjectType(), service);
                    processOutbound(component.getObjectType(), service);

                    //Check for Async reply Config
                    processReply(component.getObjectType(), service);
                }
                catch (MuleException e)
                {
                    e.printStackTrace();
                }
            }

        }
        return object;
    }

    protected void processInbound(Class componentFactoryClass, org.mule.api.service.Service service) throws MuleException
    {

        InboundEndpoint inboundEndpoint;
        List<AnnotationMetaData> annotations = AnnotationUtils.getClassAndMethodAnnotations(componentFactoryClass);
        for (AnnotationMetaData annotation : annotations)
        {
            inboundEndpoint = tryInboundEndpointAnnotation(annotation, ChannelType.Inbound);
            if (inboundEndpoint != null)
            {
                if (annotation.getType() == ElementType.METHOD)
                {
                    inboundEndpoint.getProperties().put(MuleProperties.MULE_METHOD_PROPERTY, annotation.getElementName());
                }
                ((CompositeMessageSource) service.getMessageSource()).addSource(inboundEndpoint);
            }
        }

        //Lets process the inbound routers
        processInboundRouters(componentFactoryClass, service);
    }

    protected void processInboundRouters(Class componentFactoryClass, org.mule.api.service.Service service) throws MuleException
    {
        for (int i = 0; i < componentFactoryClass.getAnnotations().length; i++)
        {
            Annotation annotation = componentFactoryClass.getAnnotations()[i];
            Router routerAnnotation = annotation.annotationType().getAnnotation(Router.class);
            if (routerAnnotation != null && routerAnnotation.type() == RouterType.Inbound)
            {
                MessageProcessorAnnotationParser parser = parserFactory.getRouterParser(annotation, componentFactoryClass, null);
                if (parser != null)
                {
                    ((ServiceCompositeMessageSource) service.getMessageSource()).addMessageProcessor(parser.parseMessageProcessor(annotation));
                }
                else
                {
                    //TODO i18n
                    throw new IllegalStateException("Cannot find parser for router annotation: " + annotation.toString());
                }
            }
        }
    }

    protected void processReplyRouters(Class componentFactoryClass, org.mule.api.service.Service service) throws MuleException
    {
        List<AnnotationMetaData> annotations = AnnotationUtils.getClassAndMethodAnnotations(componentFactoryClass);
        for (AnnotationMetaData metaData : annotations)
        {
            Router routerAnnotation = metaData.getAnnotation().annotationType().getAnnotation(Router.class);
            if (routerAnnotation != null && routerAnnotation.type() == RouterType.ReplyTo)
            {


                MessageProcessorAnnotationParser parser = parserFactory.getRouterParser(metaData.getAnnotation(), metaData.getClazz(), metaData.getMember());
                if (parser != null)
                {
                    MessageProcessor router = parser.parseMessageProcessor(metaData.getAnnotation());
                    //Todo, wrap lifecycle
                    if (router instanceof MuleContextAware)
                    {
                        ((MuleContextAware) router).setMuleContext(context);
                    }
                    if (router instanceof Initialisable)
                    {
                        ((Initialisable) router).initialise();
                    }
                    // service.getResponseRouter().addRouter(router);
                    break;
                }
                else
                {
                    //TODO i18n
                    throw new IllegalStateException("Cannot find parser for router annotation: " + metaData.getAnnotation().toString());
                }
            }
        }
    }

    protected OutboundRouter processOutboundRouter(Class componentFactoryClass) throws MuleException
    {
        Collection routerParsers = context.getRegistry().lookupObjects(MessageProcessorAnnotationParser.class);
        OutboundRouter router = null;

        List<AnnotationMetaData> annotations = AnnotationUtils.getClassAndMethodAnnotations(componentFactoryClass);
        for (AnnotationMetaData metaData : annotations)
        {
            Router routerAnnotation = metaData.getAnnotation().annotationType().getAnnotation(Router.class);
            if (routerAnnotation != null && routerAnnotation.type() == RouterType.Outbound)
            {
                if (router != null)
                {
                    //TODO i18n
                    throw new IllegalStateException("You can only configure one outbound router on a service");
                }
                MessageProcessorAnnotationParser parser = parserFactory.getRouterParser(metaData.getAnnotation(), metaData.getClazz(), metaData.getMember());
                if (parser != null)
                {
                    router = (OutboundRouter) parser.parseMessageProcessor(metaData.getAnnotation());
                }
                else
                {
                    //TODO i18n
                    throw new IllegalStateException("Cannot find parser for router annotation: " + metaData.getAnnotation().toString());
                }
            }
        }
        if (router == null)
        {
            router = new OutboundPassThroughRouter();
        }
        //Todo, wrap lifecycle
        if (router instanceof MuleContextAware)
        {
            ((MuleContextAware) router).setMuleContext(context);
        }
        router.initialise();
        return router;
    }

    protected void processOutbound(Class componentFactoryClass, org.mule.api.service.Service service) throws MuleException
    {
        OutboundRouter router = processOutboundRouter(componentFactoryClass);

        OutboundEndpoint outboundEndpoint;
        List<AnnotationMetaData> annotations = AnnotationUtils.getClassAndMethodAnnotations(componentFactoryClass);
        for (AnnotationMetaData annotation : annotations)
        {
            outboundEndpoint = tryOutboundEndpointAnnotation(annotation, ChannelType.Outbound);
            if (outboundEndpoint != null)
            {
                router.addRoute(outboundEndpoint);
            }
        }

        if (router instanceof MuleContextAware)
        {
            ((MuleContextAware) router).setMuleContext(context);
        }
        router.initialise();
        ((OutboundRouterCollection) service.getOutboundMessageProcessor()).addRoute(router);
    }

    protected InboundEndpoint tryInboundEndpointAnnotation(AnnotationMetaData metaData, ChannelType channelType) throws MuleException
    {
        Channel channelAnno = metaData.getAnnotation().annotationType().getAnnotation(Channel.class);
        if (channelAnno != null && channelAnno.type() == channelType)
        {
            EndpointAnnotationParser parser = parserFactory.getEndpointParser(metaData.getAnnotation(), metaData.getClazz(), metaData.getMember());
            if (parser == null)
            {
                //TODO i18n
                throw new AnnotationException(AnnotationsMessages.createStaticMessage("No parser found for annotation: " + metaData));
            }
            else
            {
                return parser.parseInboundEndpoint(metaData.getAnnotation(), Collections.EMPTY_MAP);
            }
        }
        return null;
    }

    protected OutboundEndpoint tryOutboundEndpointAnnotation(AnnotationMetaData metaData, ChannelType channelType) throws MuleException
    {
        Channel channelAnno = metaData.getAnnotation().annotationType().getAnnotation(Channel.class);
        if (channelAnno != null && channelAnno.type() == channelType)
        {
            EndpointAnnotationParser parser = parserFactory.getEndpointParser(metaData.getAnnotation(), metaData.getClazz(), metaData.getMember());
            if (parser == null)
            {
                //TODO i18n
                throw new AnnotationException(AnnotationsMessages.createStaticMessage("No parser found for annotation: " + metaData));
            }
            else
            {
                return parser.parseOutboundEndpoint(metaData.getAnnotation(), Collections.EMPTY_MAP);
            }
        }
        return null;
    }


    protected void processReply(Class componentFactoryClass, org.mule.api.service.Service service) throws MuleException
    {

        InboundEndpoint inboundEndpoint;
        for (int i = 0; i < componentFactoryClass.getAnnotations().length; i++)
        {
            Annotation annotation = componentFactoryClass.getAnnotations()[i];
            inboundEndpoint = tryInboundEndpointAnnotation(
                    new AnnotationMetaData(componentFactoryClass, null, ElementType.TYPE, annotation), ChannelType.Reply);
            if (inboundEndpoint != null)
            {
                service.getAsyncReplyMessageSource().addSource(inboundEndpoint);
            }
        }

        //Lets process the reply routers
        processReplyRouters(componentFactoryClass, service);
    }

    protected String getValue(String key)
    {
        return parser.parse(regProps, key);
    }

}
