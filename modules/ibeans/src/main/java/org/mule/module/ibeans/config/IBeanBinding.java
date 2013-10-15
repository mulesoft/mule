/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.config;

import org.mule.DefaultMuleEvent;
import org.mule.api.EndpointAnnotationParser;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.component.InterfaceBinding;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.ibeans.spi.MuleCallAnnotationHandler;
import org.mule.module.ibeans.spi.MuleIBeansPlugin;
import org.mule.module.ibeans.spi.support.DynamicRequestInterfaceBinding;
import org.mule.util.annotation.AnnotationMetaData;
import org.mule.util.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ibeans.annotation.Call;
import org.ibeans.annotation.Template;
import org.ibeans.annotation.param.Body;
import org.ibeans.annotation.param.BodyParam;
import org.ibeans.annotation.param.HeaderParam;
import org.ibeans.api.IBeanInvoker;
import org.ibeans.api.IBeansException;
import org.ibeans.impl.IntegrationBeanInvocationHandler;
import org.ibeans.impl.InvokeAnnotationHandler;
import org.ibeans.impl.TemplateAnnotationHandler;

/**
 * TODO
 */
public class IBeanBinding implements InterfaceBinding
{

    private static final Log logger = LogFactory.getLog(IBeanBinding.class);

    private Class interfaceClass;

    // The endpoint used to actually dispatch the message
    protected OutboundEndpoint endpoint;

    protected IBeanFlowConstruct flow;

    protected MuleIBeansPlugin plugin;
    
    protected MuleContext muleContext;

    public IBeanBinding(IBeanFlowConstruct flow, MuleContext muleContext, MuleIBeansPlugin plugin)
    {
        this.flow = flow;
        this.muleContext = muleContext;
        this.plugin = plugin;
    }

    public String getMethod()
    {
        throw new UnsupportedOperationException();
    }

    public void setMethod(String method)
    {
        throw new UnsupportedOperationException();
    }

    public MuleEvent process(MuleEvent event) throws MessagingException
    {
        try
        {
            return endpoint.process(new DefaultMuleEvent(event.getMessage(), endpoint, event.getSession()));
        }
        catch (MessagingException e)
        {
            throw e;
        }
        catch (MuleException e)
        {
            throw new MessagingException(e.getI18nMessage(), event, e);
        }
    }

    public void setInterface(Class interfaceClass)
    {
        this.interfaceClass = interfaceClass;
    }

    public Class getInterface()
    {
        return interfaceClass;
    }

    public Object createProxy(Object target)
    {
        Map<String, String> evals = new HashMap<String, String>();
        try
        {
            IBeanInvoker<MuleCallAnnotationHandler, TemplateAnnotationHandler, InvokeAnnotationHandler> invoker = plugin.getIBeanInvoker();
            invoker.getCallHandler().setFlow(flow);

            List<AnnotationMetaData> annos = AnnotationUtils.getAllMethodAnnotations(getInterface());
            for (AnnotationMetaData metaData : annos)
            {
                if (metaData.getAnnotation() instanceof Call)
                {
                    Collection c = muleContext.getRegistry().lookupObjects(EndpointAnnotationParser.class);
                    String scheme;
                    boolean http;
                    String uri = ((Call) metaData.getAnnotation()).uri();
                    int i = uri.indexOf(":/");
                    if (i == -1)
                    {
                        scheme = "dynamic";
                    }
                    else
                    {
                        scheme = uri.substring(0, i);
                    }
                    http = scheme.contains("http");

                    Map metaInfo = new HashMap();
                    //By setting the connectorName we ensure that only one connector is created for each iBean
                    metaInfo.put("connectorName", metaData.getClazz().getSimpleName() + "." + scheme); //RM*  THis affects the connector name generation + "#" + target.hashCode());

                    for (Iterator iterator = c.iterator(); iterator.hasNext();)
                    {
                        EndpointAnnotationParser parser = (EndpointAnnotationParser) iterator.next();
                        if (parser.supports(metaData.getAnnotation(), metaData.getClazz(), metaData.getMember()))
                        {
                            InterfaceBinding binding;
                            Method method = (Method) metaData.getMember();
                            boolean callChannel = false;
                            Annotation ann;
                            //This is a little messy, but we need to detect whether we are doing a Mule 'send' or Mule 'request' call.
                            //Request calls get data from a resource such as DB, email inbox or message queue. These types of request will
                            //not have any payload or headers defined.
                            //The other way to handle this is to introduce a new annotation to explicitly handle this (See the Get annotation).
                            //The issue is it may be difficult for the user to understand the difference between @Call and @Get. Instead we figure it out
                            //here.
                            for (int x = 0; x < method.getParameterAnnotations().length; x++)
                            {
                                ann = method.getParameterAnnotations()[x][0];
                                if (ann.annotationType().equals(Body.class) ||
                                        ann.annotationType().equals(BodyParam.class) ||
                                        ann.annotationType().equals(HeaderParam.class))
                                {

                                    callChannel = true;

                                    break;
                                }
                            }
                            //TODO remove the HTTP hack above. Its required becuase HTTP request on the dispatcher
                            //don't honour authenitcation for some reason.  Also even though there may not be any headers
                            //defined we still need to attach some headers to the HTTP method. This is very difficult when
                            //using request
                            if (callChannel || http)
                            {
                                OutboundEndpoint endpoint = parser.parseOutboundEndpoint(metaData.getAnnotation(), metaInfo);
                                binding = new CallInterfaceBinding(this.flow);
                                binding.setEndpoint(endpoint);
                            }
                            else
                            {
                                InboundEndpoint endpoint = parser.parseInboundEndpoint(metaData.getAnnotation(), Collections.EMPTY_MAP);
                                binding = new DynamicRequestInterfaceBinding();
                                binding.setEndpoint(endpoint);
                            }

                            binding.setInterface(getInterface());
                            binding.setMethod(metaData.getMember().toString());
                            invoker.getCallHandler().addRouterForInterface(binding);

                        }
                    }
                }
                else if (metaData.getAnnotation() instanceof Template)
                {
                    evals.put(metaData.getMember().toString(), ((Template) metaData.getAnnotation()).value());
                }
            }

            if (evals.size() > 0)
            {
                invoker.getTemplateHandler().setEvals(evals);
            }

            Object proxy = Proxy.newProxyInstance(getInterface().getClassLoader(), new Class[]{getInterface()}, createInvocationHandler());
            if (logger.isDebugEnabled())
            {
                logger.debug("Have proxy?: " + (null != proxy));
            }
            return proxy;

        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(CoreMessages.failedToCreateProxyFor(target), e);
        }
    }

    public void setEndpoint(ImmutableEndpoint e)
    {
        endpoint = (OutboundEndpoint) e;
    }

    public String toString()
    {
        final StringBuffer sb = new StringBuffer();
        sb.append("IBeanBinding");
        sb.append(", interface=").append(interfaceClass);
        sb.append('}');
        return sb.toString();
    }

    public ImmutableEndpoint getEndpoint()
    {
        return endpoint;
    }

    protected InvocationHandler createInvocationHandler() throws IBeansException
    {
        return new IntegrationBeanInvocationHandler(interfaceClass, plugin);
    }
}
