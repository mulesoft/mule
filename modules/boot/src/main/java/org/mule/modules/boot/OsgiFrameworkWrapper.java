/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.boot;

import org.knopflerfish.framework.Main;
import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class OsgiFrameworkWrapper implements WrapperListener
{
    private final String DESKTOP_BUNDLE = "boot/osgi/desktop/desktop_all-2.0.0.jar";
    
    private final boolean startGui;

    /*---------------------------------------------------------------
     * Constructors
     *-------------------------------------------------------------*/
    public OsgiFrameworkWrapper(boolean startGui)
    {
        this.startGui = startGui;
    }

    /*---------------------------------------------------------------
     * WrapperListener Methods
     *-------------------------------------------------------------*/
    /**
     * The start method is called when the WrapperManager is signaled by the
     *    native wrapper code that it can start its application.  This
     *    method call is expected to return, so a new thread should be launched
     *    if necessary.
     *
     * @param args List of arguments used to initialize the application.
     *
     * @return Any error code if the application should exit on completion
     *         of the start method.  If there were no problems then this
     *         method should return null.
     */
    public Integer start( String[] args )
    {
        Properties osgiSysProps = new Properties();
        // Directory where bundle state info. is stored.
        // .xargs files will be read from the parent of this directory.
        osgiSysProps.put("org.osgi.framework.dir", "../conf/knopflerfish");

        for (Iterator iter = osgiSysProps.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            System.setProperty((String) entry.getKey(), (String) entry.getValue());
        }

        try {
            Main.main(args);
            if (startGui) {
                Main.main(new String[]{"-istart", DESKTOP_BUNDLE});
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return new Integer(1);
        }
    }

    /**
     * Called when the application is shutting down.  The Wrapper assumes that
     *  this method will return fairly quickly.  If the shutdown code code
     *  could potentially take a long time, then WrapperManager.signalStopping()
     *  should be called to extend the timeout period.  If for some reason,
     *  the stop method can not return, then it must call
     *  WrapperManager.stopped() to avoid warning messages from the Wrapper.
     *
     * @param exitCode The suggested exit code that will be returned to the OS
     *                 when the JVM exits.
     *
     * @return The exit code to actually return to the OS.  In most cases, this
     *         should just be the value of exitCode, however the user code has
     *         the option of changing the exit code if there are any problems
     *         during shutdown.
     */
    public int stop( int exitCode )
    {
        Main.shutdown(exitCode);
        return exitCode;
    }

    /**
     * Called whenever the native wrapper code traps a system control signal
     *  against the Java process.  It is up to the callback to take any actions
     *  necessary.  Possible values are: WrapperManager.WRAPPER_CTRL_C_EVENT,
     *    WRAPPER_CTRL_CLOSE_EVENT, WRAPPER_CTRL_LOGOFF_EVENT, or
     *    WRAPPER_CTRL_SHUTDOWN_EVENT
     *
     * @param event The system control signal.
     */
    public void controlEvent( int event )
    {
        if (WrapperManager.isControlledByNativeWrapper()) {
            // The Wrapper will take care of this event
        } else {
            // We are not being controlled by the Wrapper, so
            //  handle the event ourselves.
            if ((event == WrapperManager.WRAPPER_CTRL_C_EVENT) ||
                (event == WrapperManager.WRAPPER_CTRL_CLOSE_EVENT) ||
                (event == WrapperManager.WRAPPER_CTRL_SHUTDOWN_EVENT)){
                WrapperManager.stop(0);
            }
        }
    }
}