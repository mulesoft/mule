/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.client;

import org.mule.MuleException;
import org.mule.extras.client.i18n.ClientMessages;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.transformer.UMOTransformer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * <code>MuleProxyListener</code> is a generic listent proxy that can be used to
 * foward calls as Mule events from any Observer/Observerable implementation.
 */

public class MuleProxyListener implements InvocationHandler
{
    private Class listenerClass;
    private AbstractEventTransformer eventTransformer;
    private String componentName;
    private Object proxy;
    private MuleClient client;

    public MuleProxyListener(Class listenerClass, String componentName) throws UMOException
    {
        setListenerClass(listenerClass);
        setEventTransformer(new EventObjectTransformer());
        setComponentName(componentName);
        setClient(new MuleClient());
        createProxy();
    }

    public MuleProxyListener(Class listenerClass,
                             AbstractEventTransformer eventTransformer,
                             String componentName) throws UMOException
    {
        setListenerClass(listenerClass);
        setEventTransformer(eventTransformer);
        setComponentName(componentName);
        setClient(new MuleClient());
        createProxy();
    }

    public MuleProxyListener(Class listenerClass,
                             AbstractEventTransformer eventTransformer,
                             String componentName,
                             MuleClient client)
    {
        setListenerClass(listenerClass);
        setEventTransformer(eventTransformer);
        setComponentName(componentName);
        setClient(client);
        createProxy();
    }

    protected void createProxy()
    {
        proxy = Proxy.newProxyInstance(listenerClass.getClassLoader(), new Class[]{listenerClass}, this);
    }

    public Class getListenerClass()
    {
        return listenerClass;
    }

    public void setListenerClass(Class listenerClass)
    {
        this.listenerClass = listenerClass;
    }

    public UMOTransformer getEventTransformer()
    {
        return eventTransformer;
    }

    public void setEventTransformer(AbstractEventTransformer eventTransformer)
    {
        this.eventTransformer = eventTransformer;
    }

    public String getComponentName()
    {
        return componentName;
    }

    public void setComponentName(String componentName)
    {
        this.componentName = componentName;
    }

    public MuleClient getClient()
    {
        return client;
    }

    public void setClient(MuleClient client)
    {
        this.client = client;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        if (args.length == 0)
        {
            throw new MuleException(ClientMessages.noArgsForProxy());
        }
        UMOMessage message = eventTransformer.transform(args[0], method);
        if (!"void".equals(method.getReturnType().getName()))
        {
            UMOMessage result = client.sendDirect(componentName, null, message);
            if (UMOMessage.class.equals(method.getReturnType()))
            {
                return result;
            }
            else
            {
                return (result == null ? null : result.getPayload());
            }
        }
        else
        {
            client.dispatchDirect(componentName, message);
            return null;
        }
    }

    public Object getProxy()
    {
        return proxy;
    }
}
