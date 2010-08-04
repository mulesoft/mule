/*
 * $Id: CallAnnotationHandler.java 343 2010-05-05 05:43:44Z ross $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.spi;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.routing.InterfaceBinding;
import org.mule.config.i18n.CoreMessages;
import org.mule.message.DefaultExceptionPayload;
import org.mule.transport.NullPayload;
import org.mule.util.StringMessageUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ibeans.api.ClientAnnotationHandler;
import org.ibeans.api.InvocationContext;
import org.ibeans.api.Request;
import org.ibeans.api.Response;

/**
 * Used to Handle {@link org.ibeans.annotation.Call} annotated method calls.
 */
public class MuleCallAnnotationHandler implements ClientAnnotationHandler
{
    public static final String DEFAULT_METHOD_NAME_TOKEN = "default";

    protected static Log logger = LogFactory.getLog(MuleCallAnnotationHandler.class);

    private MuleContext muleContext;

    protected Map<String, InterfaceBinding> routers = new HashMap<String, InterfaceBinding>();

    public MuleCallAnnotationHandler(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public void addRouterForInterface(InterfaceBinding router)
    {
        if (router instanceof MuleContextAware)
        {
            ((MuleContextAware) router).setMuleContext(muleContext);
        }
        if (router.getMethod() == null)
        {
            if (routers.size() == 0)
            {
                routers.put(DEFAULT_METHOD_NAME_TOKEN, router);
            }
            else
            {
                throw new IllegalArgumentException(CoreMessages.mustSetMethodNamesOnBinding().getMessage());
            }
        }
        else
        {
            routers.put(router.getMethod(), router);
        }


    }

    public Response invoke(InvocationContext ctx) throws Exception
    {
        InterfaceBinding router = routers.get(ctx.getMethod().toString());
        if (router == null)
        {
            throw new IllegalArgumentException(CoreMessages.cannotFindBindingForMethod(ctx.getMethod().getName()).toString());
        }
        router.getEndpoint().getProperties().putAll(ctx.getIBeanDefaultConfig().getPropertyParams());
        Request req = ctx.getRequest();
        MuleMessage message = ((MuleRequestMessage)req).getMessage();

        if (logger.isTraceEnabled())
        {
            try
            {
                logger.trace("Message Before invoking "
                        + ctx.getMethod()
                        + ": \n"
                        + StringMessageUtils.truncate(
                        StringMessageUtils.toString(message.getPayload()),
                        2000, false));
                logger.trace("Message Headers: \n"
                        + StringMessageUtils.headersToString(message));
            }
            catch (Exception e)
            {
                // ignore
            }
        }

//        MuleMessage message = new DefaultMuleMessage(req.getPayload(), muleContext);
//
//        message.addProperties(router.getEndpoint().getProperties(), PropertyScope.INVOCATION);
//        for (String s : req.getHeaderNames())
//        {
//            message.setOutboundProperty(s, req.getHeader(s));
//        }
//        for (String s : req.getAttachmentNames())
//        {
//            message.addAttachment(s, req.getAttachment(s));
//        }
       

        MuleMessage reply;
        MuleSession session = null; //new DefaultMuleSession(service, muleContext);

        try
        {
            reply = router.route(message, session);
        }
        catch (Throwable e)
        {
            //Make all exceptions go through the CallException handler
            reply = new DefaultMuleMessage(NullPayload.getInstance(), muleContext);
            reply.setExceptionPayload(new DefaultExceptionPayload(e));
        }
        return new MuleResponseMessage(reply);
    }

    public String getScheme(Method method)
    {
        InterfaceBinding router = routers.get(method.toString());
        if (router == null)
        {
            throw new IllegalArgumentException(CoreMessages.cannotFindBindingForMethod(method.getName()).toString());
        }
        return router.getEndpoint().getEndpointURI().getScheme();
    }

    ImmutableEndpoint getEndpointForMethod(Method method)
    {
        InterfaceBinding router = routers.get(method.toString());
        if (router != null)
        {
            return router.getEndpoint();
        }
        return null;
    }
}