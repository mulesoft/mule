/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.xfire.service.binding.ObjectServiceFactory;
import org.codehaus.xfire.transport.TransportManager;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.util.ClassUtils;

/**
 * TODO document
 */
public class MuleObjectServiceFactory extends ObjectServiceFactory
{

    protected final Set excludedMethods = new HashSet();

    /**
     * Initializes a new instance of the <code>ObjectServiceFactory</code>.
     */
    public MuleObjectServiceFactory(TransportManager transportManager)
    {
        super(transportManager);
        initExcludedMethods();
    }

    protected void initExcludedMethods()
    {
        // JDK methods to be ignored
        addIgnoredMethods("java.lang.Object");
        addIgnoredMethods("java.lang.Throwable");
        addIgnoredMethods("org.omg.CORBA_2_3.portable.ObjectImpl");
        addIgnoredMethods("org.omg.CORBA.portable.ObjectImpl");
        addIgnoredMethods("javax.ejb.EJBObject");
        addIgnoredMethods("javax.rmi.CORBA.Stub");

        // Mule methods to be ignored
        addIgnoredMethods(Callable.class.getName());
        addIgnoredMethods(Initialisable.class.getName());
        addIgnoredMethods(Disposable.class.getName());
    }

    /**
     * Ignore the specified class' declared methods. This can be used to not expose
     * certain interfaces as a service. By default, the methods specified by the
     * following interfaces/classes are ignored:
     * <li><code>java.lang.Object</code>
     * <li><code>org.omg.CORBA_2_3.portable.ObjectImpl</code>
     * <li><code>org.omg.CORBA.portable.ObjectImpl</code>
     * <li><code>javax.ejb.EJBObject</code>
     * <li><code>javax.ejb.EJBLocalObject</code>
     * <li><code>javax.rmi.CORBA.Stub</code>
     * <li><code>org.mule.umo.lifecycle.Callable</code>
     * <li><code>org.mule.umo.lifecycle.Initialisable</code>
     * <li><code>org.mule.umo.lifecycle.Disposable</code>
     * 
     * @param className the fully qualified class name
     */
    public void addIgnoredMethods(String className)
    {
        try
        {
            Class c = ClassUtils.loadClass(className, getClass());
            for (int i = 0; i < c.getMethods().length; i++)
            {
                excludedMethods.add(getMethodName(c.getMethods()[i]));
            }
        }
        catch (ClassNotFoundException e)
        {
            // can be ignored.
        }
    }

    protected boolean isValidMethod(final Method method)
    {
        if (excludedMethods.contains(getMethodName(method))) return false;

        final int modifiers = method.getModifiers();

        return Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers);
    }

    protected String getMethodName(Method method)
    {
        return method.getName();
    }

}
