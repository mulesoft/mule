/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.client;

import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.transformer.UMOTransformer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * <code>MuleProxyListener</code> is a generic listent proxy that can be used
 * to foward calls as Mule events from any Observer/Observerable implementation.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
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

    public MuleProxyListener(Class listenerClass, AbstractEventTransformer eventTransformer, String componentName) throws UMOException
    {
        setListenerClass(listenerClass);
        setEventTransformer(eventTransformer);
        setComponentName(componentName);
        setClient(new MuleClient());
        createProxy();
    }

    public MuleProxyListener(Class listenerClass, AbstractEventTransformer eventTransformer, String componentName, MuleClient client)
    {
        setListenerClass(listenerClass);
        setEventTransformer(eventTransformer);
        setComponentName(componentName);
        setClient(client);
        createProxy();
    }

    protected void createProxy() {
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
        if(args.length==0) {
            throw new MuleClientException("There is not event specified in the inoke args. args length is zero");
        }
        UMOMessage message = eventTransformer.transform(args[0], method);
        if(!"void".equals(method.getReturnType().getName()))
        {
            UMOMessage result = client.sendDirect(componentName, null, message.getPayload(), message.getProperties());
            if(UMOMessage.class.equals(method.getReturnType())) {
                return result;
            }else {
                return (result==null ? null : result.getPayload());
            }
        } else {
            client.dispatchDirect(componentName, message.getPayload(), message.getProperties());
            return null;
        }
    }

    public Object getProxy() {
        return proxy;
    }
}
