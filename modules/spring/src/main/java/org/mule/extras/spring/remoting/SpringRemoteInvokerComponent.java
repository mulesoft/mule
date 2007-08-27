/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.spring.remoting;

import org.mule.config.i18n.CoreMessages;
import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.ClassUtils;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationBasedExporter;
import org.springframework.remoting.support.RemoteInvocationExecutor;
import org.springframework.remoting.support.RemoteInvocationResult;

public class SpringRemoteInvokerComponent implements Initialisable, Callable
{
    private Delegate delegate;
    private Class serviceClass;
    private Class serviceInterface;
    private Object serviceBean;
    private boolean registerTraceInterceptor = false;
    private RemoteInvocationExecutor remoteInvocationExecutor;

    private class Delegate extends RemoteInvocationBasedExporter implements InitializingBean
    {
        private Object proxy;

        public void afterPropertiesSet()
        {
            this.proxy = getProxyForService();
        }

        public Object execute(RemoteInvocation invocation)
        {
            try
            {
                Object value = invoke(invocation, proxy);
                return value;
            }
            catch (Throwable ex)
            {
                ex.printStackTrace();
                return new RemoteInvocationResult(ex);
            }
        }
    }

    public SpringRemoteInvokerComponent()
    {
        delegate = new Delegate();
    }

    public void initialise() throws InitialisationException {
        if (serviceClass == null && serviceBean == null)
        {
            throw new InitialisationException(
                CoreMessages.propertiesNotSet("serviceClass or serviceBean"), this);
        }
        if (serviceInterface == null)
        {
            throw new InitialisationException(
                CoreMessages.propertiesNotSet("serviceInterface"), this);
        }

        if (serviceClass != null)
        {
            Object service = null;
            try
            {
                service = ClassUtils.instanciateClass(serviceClass, null);
            }
            catch (Exception e)
            {
                throw new InitialisationException(e, this);
            }
            delegate.setService(service);
        }
        else if (serviceBean != null)
        {
            delegate.setService(serviceBean);
        }
        delegate.setServiceInterface(serviceInterface);
        delegate.setRegisterTraceInterceptor(registerTraceInterceptor);
        if (remoteInvocationExecutor != null)
        {
            delegate.setRemoteInvocationExecutor(remoteInvocationExecutor);
        }
        delegate.afterPropertiesSet();
    }

    public Class getServiceClass()
    {
        return serviceClass;
    }

    public void setServiceClass(Class serviceClass)
    {
        this.serviceClass = serviceClass;
    }

    public Object getServiceBean()
    {
        return serviceBean;
    }

    public void setServiceBean(Object serviceBean)
    {
        this.serviceBean = serviceBean;
    }

    public Class getServiceInterface()
    {
        return serviceInterface;
    }

    public void setServiceInterface(Class serviceInterface)
    {
        this.serviceInterface = serviceInterface;
    }

    public boolean isRegisterTraceInterceptor()
    {
        return registerTraceInterceptor;
    }

    public void setRegisterTraceInterceptor(boolean registerTraceInterceptor)
    {
        this.registerTraceInterceptor = registerTraceInterceptor;
    }

    public RemoteInvocationExecutor getRemoteInvocationExecutor()
    {
        return remoteInvocationExecutor;
    }

    public void setRemoteInvocationExecutor(RemoteInvocationExecutor remoteInvocationExecutor)
    {
        this.remoteInvocationExecutor = remoteInvocationExecutor;
    }

    public Object onCall(UMOEventContext eventContext) throws Exception
    {
        Object transformedMessage = eventContext.getTransformedMessage();
        RemoteInvocation ri = (RemoteInvocation)transformedMessage;
        Object rval = delegate.execute(ri);
        return rval;
    }
}
