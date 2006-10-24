/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.mule.config.ExceptionHelper;
import org.mule.config.MuleProperties;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.providers.soap.ServiceProxy;
import org.mule.umo.UMOException;
import org.mule.umo.UMOExceptionPayload;
import org.mule.umo.UMOMessage;
import org.mule.umo.provider.UMOMessageAdapter;

/**
 * <code>ServiceProxy</code> is a proxy that wraps a soap endpointUri to look like
 * a Web service. Also provides helper methods for building and describing web
 * service interfaces in Mule.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class AxisServiceProxy extends ServiceProxy
{

    public static Object createProxy(AbstractMessageReceiver receiver, boolean synchronous, Class[] classes)
    {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return Proxy.newProxyInstance(cl, classes, createServiceHandler(receiver, synchronous));
    }

    public static InvocationHandler createServiceHandler(AbstractMessageReceiver receiver, boolean synchronous)
    {
        return new AxisServiceHandler(receiver, synchronous);
    }

    private static class AxisServiceHandler implements InvocationHandler
    {
        private AbstractMessageReceiver receiver;
        private boolean synchronous = true;

        public AxisServiceHandler(AbstractMessageReceiver receiver, boolean synchronous)
        {
            this.receiver = receiver;
            this.synchronous = synchronous;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            UMOMessageAdapter messageAdapter = receiver.getConnector().getMessageAdapter(args);
            messageAdapter.setProperty(MuleProperties.MULE_METHOD_PROPERTY, method);

            UMOMessage message = receiver.routeMessage(new MuleMessage(messageAdapter), synchronous);

            if (message != null)
            {
                UMOExceptionPayload wsException = message.getExceptionPayload();

                if (wsException != null)
                {
                    UMOException umoException = ExceptionHelper.getRootMuleException(wsException.getException());
                    // if the exception has a cause, then throw only the cause
                    if (umoException.getCause() != null)
                    {
                        throw umoException.getCause();
                    }
                    else
                    {
                        throw umoException;
                    }
                }

                return message.getPayload();
            }
            else
            {
                return null;
            }
        }
    }
}
