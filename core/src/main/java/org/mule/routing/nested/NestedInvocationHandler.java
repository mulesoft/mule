/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.nested;

import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.routing.NestedRouter;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.NullPayload;
import org.mule.util.StringMessageUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class NestedInvocationHandler implements InvocationHandler
{

    public static final String DEFAULT_METHOD_NAME_TOKEN = "default";

    protected static Log logger = LogFactory.getLog(NestedInvocationHandler.class);

    protected Map routers = new ConcurrentHashMap();

    public NestedInvocationHandler(NestedRouter router)
    {
        addRouterForInterface(router);
    }

    public void addRouterForInterface(NestedRouter router)
    {
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

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        if (method.getName().equals("toString"))
        {
            return toString();
        }

        MuleMessage message;

        if(args==null)
        {
            message = new DefaultMuleMessage(NullPayload.getInstance());
        }
        else if (args.length == 1)
        {
            message = new DefaultMuleMessage(args[0]);
        }
        else
        {
            message = new DefaultMuleMessage(args);
        }


        NestedRouter router = (NestedRouter) routers.get(method.getName());
        if (router == null)
        {
            router = (NestedRouter) routers.get(DEFAULT_METHOD_NAME_TOKEN);
        }

        if (router == null)
        {
            throw new IllegalArgumentException(CoreMessages.cannotFindBindingForMethod(method.getName()).toString());
        }

        MuleMessage reply;
        MuleEvent currentEvent = RequestContext.getEvent();
        reply = router.route(message, currentEvent.getSession());

        if (reply != null)
        {
            if (reply.getExceptionPayload() != null)
            {
                throw reply.getExceptionPayload().getException();
            }
            else
            {
                return reply.getPayload();
            }
        }
        else
        {
            return null;
        }
    }

    public String toString()
    {
        final StringBuffer sb = new StringBuffer();
        sb.append("NestedInvocation");
        sb.append("{routers='").append(StringMessageUtils.toString(routers));
        sb.append('}');
        return sb.toString();
    }
}
