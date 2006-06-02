/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.soap.glue;

import electric.service.IService;

import org.mule.config.MuleProperties;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.providers.soap.ServiceProxy;
import org.mule.umo.UMOMessage;
import org.mule.umo.provider.UMOMessageAdapter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * <code>ServiceProxy</code> is a proxy that wraps a soap endpointUri to look
 * like a Web service.
 * 
 * Also provides helper methods for building and describing web service
 * interfaces in Mule.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class GlueServiceProxy extends ServiceProxy
{
    public static Object createProxy(AbstractMessageReceiver receiver, boolean synchronous, Class[] classes)
    {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return Proxy.newProxyInstance(cl,
                                      classes,
                                      createServiceHandler(receiver, synchronous));
    }

    public static InvocationHandler createServiceHandler(AbstractMessageReceiver receiver, boolean synchronous)
    {
        return new GlueServiceHandler(receiver, synchronous);
    }

    private static class GlueServiceHandler implements InvocationHandler
    {
        private AbstractMessageReceiver receiver;
        private boolean synchronous = true;

        public GlueServiceHandler(AbstractMessageReceiver receiver, boolean synchronous)
        {
            this.receiver = receiver;
            this.synchronous = synchronous;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            GlueMessageAdapter.GlueMessageHolder holder;
            if (args.length == 1) {
                holder = new GlueMessageAdapter.GlueMessageHolder(args[0], (IService) proxy);
            } else {
                holder = new GlueMessageAdapter.GlueMessageHolder(args, (IService) proxy);
            }
            UMOMessageAdapter messageAdapter = receiver.getConnector().getMessageAdapter(holder);
            messageAdapter.setProperty(MuleProperties.MULE_METHOD_PROPERTY, method);

            UMOMessage message = receiver.routeMessage(new MuleMessage(messageAdapter), synchronous);

            if (message != null) {
                if(message.getExceptionPayload()!=null) {
                    throw message.getExceptionPayload().getException();
                }
                return message.getPayload();
            } else {
                return null;
            }
        }
    }
}
