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

import org.mule.config.i18n.CoreMessages;
import org.mule.impl.MuleMessage;
import org.mule.impl.RequestContext;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.routing.UMONestedRouter;

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

    protected NestedInvocationHandler(UMONestedRouter router)
    {
        addRouterForInterface(router);
    }

    public void addRouterForInterface(UMONestedRouter router)
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

        UMOMessage message = new MuleMessage(args);
        UMONestedRouter router = (UMONestedRouter) routers.get(method.getName());
        if (router == null)
        {
            router = (UMONestedRouter) routers.get(DEFAULT_METHOD_NAME_TOKEN);
        }

        if (router == null)
        {
            throw new IllegalArgumentException(
                CoreMessages.cannotFindBindingForMethod(method.getName()).toString());
        }

        UMOMessage reply;

        UMOEvent currentEvent = RequestContext.getEvent();

        reply = router.route(message, currentEvent.getSession(), currentEvent.isSynchronous());

        if (reply != null)
        {
            return reply.getPayload();
        }
        else
        {
            return null;
        }
    }
}
