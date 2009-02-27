/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.axis;

import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.ExceptionPayload;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.component.JavaComponent;
import org.mule.api.config.MuleProperties;
import org.mule.api.lifecycle.Callable;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.service.Service;
import org.mule.api.transport.MessageAdapter;
import org.mule.config.ExceptionHelper;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.soap.SoapConstants;
import org.mule.transport.soap.axis.extras.AxisCleanAndAddProperties;
import org.mule.util.ClassUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * <code>ServiceProxy</code> is a proxy that wraps a soap endpointUri to look like
 * a Web service. Also provides helper methods for building and describing web
 * service interfaces in Mule.
 */

public class AxisServiceProxy
{

    private static ThreadLocal properties = new ThreadLocal();

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
            MessageAdapter messageAdapter = receiver.getConnector().getMessageAdapter(args);
            messageAdapter.setProperty(MuleProperties.MULE_METHOD_PROPERTY, method);
            
            // add all custom headers, filter out all mule headers (such as
            // MULE_SESSION) except
            // for MULE_USER header. Filter out other headers like "soapMethods" and
            // MuleProperties.MULE_METHOD_PROPERTY and "soapAction"
            // and also filter out any http related header
            messageAdapter.addProperties(AxisCleanAndAddProperties.cleanAndAdd(RequestContext.getEventContext()));                        
                                   
            MuleMessage message = receiver.routeMessage(new DefaultMuleMessage(messageAdapter), synchronous);
            
            if (message != null)
            {
                ExceptionPayload wsException = message.getExceptionPayload();

                if (wsException != null)
                {
                    MuleException exception = ExceptionHelper.getRootMuleException(wsException.getException());
                    // if the exception has a cause, then throw only the cause
                    if (exception.getCause() != null)
                    {
                        throw exception.getCause();
                    }
                    else
                    {
                        throw exception;
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

    /*
       This is a horrible hack, which is axis-specific (no general classes are affected).  It was
       added to allow service interface to be configured on endpoints.  The reason it needs to be
       via a global thread local is that:
       - the routine getInterfacesForComponent is called from "callback" objects, of which at least
         one is set in the axis connector.  So the endpoint properties are unavailable when set.
       - the information passed to the callback is sufficient to identify the component, but not
         the endpoint, and we would like this configuration to be endpoint specific for two
         reasons: (i) it is more flexible and (ii) we want to avoid transport specific config
         on the component (setting it on the connector is way too constraining)
       - the only other solution (which also uses thread local globals) would be to use the
         request context, but this is called, amongst other places, from the create() method
         of the axis message receiver, so no request context is currently in scope.
       I apologise for this poor code, but after discussing it with rest of the 2.x team we
       decided that if it worked, it was probably sufficient, since axis 1 support is largely
       legacy-based.  AC.
     */
    public static void setProperties(Map properties)
    {
        AxisServiceProxy.properties.set(properties);
    }

    public static Class[] getInterfacesForComponent(Service service)
        throws MuleException, ClassNotFoundException
    {
        Class[] interfaces;
        List ifaces = null;

        Map localProperties = (Map) properties.get();
        if (null != localProperties)
        {
            ifaces = (List) localProperties.get(SoapConstants.SERVICE_INTERFACES);
        }
        if (ifaces == null || ifaces.size() == 0)
        {
            final Class implementationClass;

            if (service.getComponent() instanceof JavaComponent)
            {
                try
                {
                    implementationClass = ((JavaComponent) service.getComponent()).getObjectType();
                }
                catch (Exception e)
                {
                    throw new ClassNotFoundException("Unable to retrieve class from service factory", e);
                }
            }
            else
            {
                throw new ClassNotFoundException("Unable to retrieve class from service factory");
            }
            

            // get all implemented interfaces from superclasses as well
            final List intfList = ClassUtils.getAllInterfaces(implementationClass);
            interfaces = (Class[])intfList.toArray(new Class[intfList.size()]);
        }
        else
        {
            interfaces = new Class[ifaces.size()];
            for (int i = 0; i < ifaces.size(); i++)
            {
                String iface = (String)ifaces.get(i);
                interfaces[i] = ClassUtils.loadClass(iface, AxisServiceProxy.class);
            }
        }

        interfaces = removeInterface(interfaces, Callable.class);
        interfaces = removeInterface(interfaces, Disposable.class);
        interfaces = removeInterface(interfaces, Initialisable.class);
        return interfaces;
    }

    public static Class[] removeInterface(Class[] interfaces, Class iface)
    {
        if (interfaces == null)
        {
            return null;
        }
        List results = new ArrayList();
        for (int i = 0; i < interfaces.length; i++)
        {
            Class anInterface = interfaces[i];
            if (!anInterface.equals(iface))
            {
                results.add(anInterface);
            }
        }
        Class[] arResults = new Class[results.size()];
        if (arResults.length == 0)
        {
            return arResults;
        }
        else
        {
            results.toArray(arResults);
            return arResults;
        }
    }

    public static Method[] getMethods(Class[] interfaces)
    {
        List methodNames = new ArrayList();
        for (int i = 0; i < interfaces.length; i++)
        {
            methodNames.addAll(Arrays.asList(interfaces[i].getMethods()));
        }
        Method[] results = new Method[methodNames.size()];
        return (Method[])methodNames.toArray(results);

    }

    public static String[] getMethodNames(Class[] interfaces)
    {
        Method[] methods = getMethods(interfaces);

        String[] results = new String[methods.length];
        for (int i = 0; i < results.length; i++)
        {
            results[i] = methods[i].getName();
        }
        return results;
    }

}
