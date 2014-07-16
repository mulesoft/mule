/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.boot;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;

public class MuleServerWrapper implements WrapperListener
{

    /**
     * We can't reference MuleServer class literal here, as it will fail to resolve at runtime.
     * Instead, make all calls anonymous through reflection, so we can safely pump up our new classloader
     * and make it the default one for downstream applications. 
     */
    private Object mule;

    /*---------------------------------------------------------------
     * Constructors
     *-------------------------------------------------------------*/
    public MuleServerWrapper()
    {
        super();
    }

    /*---------------------------------------------------------------
     * WrapperListener Methods
     *-------------------------------------------------------------*/
    /**
     * The start method is called when the WrapperManager is signaled by the native
     * wrapper code that it can start its application. This method call is expected
     * to return, so a new thread should be launched if necessary.
     * 
     * @param args List of arguments used to initialize the application.
     * @return Any error code if the application should exit on completion of the
     *         start method. If there were no problems then this method should return
     *         null.
     */
    public Integer start(String[] args)
    {
        try
        {
            ClassLoader muleSystemCl = new MuleSystemClassLoader();

            Thread.currentThread().setContextClassLoader(muleSystemCl);

            Class muleClass = Thread.currentThread().getContextClassLoader().loadClass("org.mule.MuleServer");
            Constructor c = muleClass.getConstructor(String[].class);
            mule = c.newInstance(new Object[] {args});
            Method startMethod = muleClass.getMethod("start", boolean.class, boolean.class);
            startMethod.invoke(mule, false, false);
            return null;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return Integer.valueOf(1);
        }
    }

    /**
     * Called when the application is shutting down. The Wrapper assumes that this
     * method will return fairly quickly. If the shutdown code code could potentially
     * take a long time, then WrapperManager.signalStopping() should be called to
     * extend the timeout period. If for some reason, the stop method can not return,
     * then it must call WrapperManager.stopped() to avoid warning messages from the
     * Wrapper.
     * 
     * @param exitCode The suggested exit code that will be returned to the OS when
     *            the JVM exits.
     * @return The exit code to actually return to the OS. In most cases, this should
     *         just be the value of exitCode, however the user code has the option of
     *         changing the exit code if there are any problems during shutdown.
     */
    public int stop(int exitCode)
    {
        try
        {
            Method shutdownMethod = mule.getClass().getMethod("shutdown");
            shutdownMethod.invoke(mule);
        }
        catch (Throwable t)
        {
            // ignore
        }
        
        return exitCode;
    }

    /**
     * Called whenever the native wrapper code traps a system control signal against
     * the Java process. It is up to the callback to take any actions necessary.
     * Possible values are: WrapperManager.WRAPPER_CTRL_C_EVENT,
     * WRAPPER_CTRL_CLOSE_EVENT, WRAPPER_CTRL_LOGOFF_EVENT, or
     * WRAPPER_CTRL_SHUTDOWN_EVENT
     * 
     * @param event The system control signal.
     */
    public void controlEvent(int event)
    {
        if (WrapperManager.isControlledByNativeWrapper())
        {
            // The Wrapper will take care of this event
        }
        else
        {
            // We are not being controlled by the Wrapper, so
            // handle the event ourselves.
            if ((event == WrapperManager.WRAPPER_CTRL_C_EVENT)
                || (event == WrapperManager.WRAPPER_CTRL_CLOSE_EVENT)
                || (event == WrapperManager.WRAPPER_CTRL_SHUTDOWN_EVENT))
            {
                WrapperManager.stop(0);
            }
        }
    }

}
