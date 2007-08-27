/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap;

import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <code>ServiceProxy</code> is a proxy that wraps a soap endpointUri to look like
 * a Web service. Also provides helper methods for building and describing web
 * service interfaces in Mule.
 */

public class ServiceProxy
{

    public static Class[] getInterfacesForComponent(UMOComponent component)
        throws UMOException, ClassNotFoundException
    {
        Class[] interfaces;
        List ifaces = (List)component.getDescriptor().getProperties().get("serviceInterfaces");
        if (ifaces == null || ifaces.size() == 0)
        {
            final Class implementationClass;
            try
            {
                implementationClass = component.getDescriptor().getServiceFactory().create().getClass();
            }
            catch (Exception e)
            {
                throw new ClassNotFoundException("Unable to retrieve class from service factory", e);
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
                interfaces[i] = ClassUtils.loadClass(iface, ServiceProxy.class);
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
