/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.management.agent;

import javax.management.MBeanException;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.StandardMBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Ensures any external jmx invocation (like e.g. remote) is executed with a correct application
 * classloader (otherwise a bootstrap classloader is used by default for platform mbean server). Note
 * the irony - extends StandardMBean, but StandardMBean is not your 'standard mbean', but rather a
 * special kind of the DynamicMBean which generates attributes/operations based on the passed in
 * interface (via reflection).
 */
public class ClassloaderSwitchingMBeanWrapper extends StandardMBean implements MBeanRegistration
{
    protected Log logger = LogFactory.getLog(getClass());

    private ClassLoader executionClassLoader;

    public <T> ClassloaderSwitchingMBeanWrapper(T implementation, Class<T> mbeanInterface, ClassLoader executionClassLoader)
            throws NotCompliantMBeanException
    {
        super(implementation, mbeanInterface);
        this.executionClassLoader = executionClassLoader;
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException
    {
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader(executionClassLoader);
            return super.invoke(actionName, params, signature);
        }
        catch (MBeanException mbex)
        {
            throw mbex;
        }
        catch (ReflectionException rex)
        {
            throw rex;
        }
        catch (Exception ex)
        {
            logger.error(String.format("MBean operation '%s' failed", actionName), ex);
            if (ex instanceof RuntimeException)
            {
                throw (RuntimeException) ex;
            }

            throw new RuntimeException(ex);
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    public ClassLoader getExecutionClassLoader()
    {
        return executionClassLoader;
    }

    public void setExecutionClassLoader(ClassLoader executionClassLoader)
    {
        this.executionClassLoader = executionClassLoader;
    }

    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception
    {
        if (getImplementation() instanceof MBeanRegistration)
        {
            return ((MBeanRegistration) getImplementation()).preRegister(server, name);
        }

        return name;
    }

    public void postRegister(Boolean registrationDone)
    {
        if (getImplementation() instanceof MBeanRegistration)
        {
            ((MBeanRegistration) getImplementation()).postRegister(registrationDone);
        }
    }

    public void preDeregister() throws Exception
    {
        if (getImplementation() instanceof MBeanRegistration)
        {
            ((MBeanRegistration) getImplementation()).preDeregister();
        }
    }

    public void postDeregister()
    {
        if (getImplementation() instanceof MBeanRegistration)
        {
            ((MBeanRegistration) getImplementation()).postDeregister();
        }
    }
}
