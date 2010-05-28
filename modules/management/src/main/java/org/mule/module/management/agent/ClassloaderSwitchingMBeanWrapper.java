package org.mule.module.management.agent;

import javax.management.MBeanException;
import javax.management.NotCompliantMBeanException;
import javax.management.ReflectionException;
import javax.management.StandardMBean;

/**
 * Ensures any external jmx invocation (like e.g. remote) is executed with a correct application
 * classloader (otherwise a bootstrap classloader is used by default for platform mbean server). Note
 * the irony - extends StandardMBean, but StandardMBean is not your 'standard mbean', but rather a
 * special kind of the DynamicMBean which generates attributes/operations based on the passed in
 * interface (via reflection).
 */
public class ClassloaderSwitchingMBeanWrapper extends StandardMBean
{

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
}
