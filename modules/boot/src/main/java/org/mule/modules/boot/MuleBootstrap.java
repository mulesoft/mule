/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.modules.boot;

import org.mule.MuleServer;
import org.mule.util.SystemUtils;
import org.tanukisoftware.wrapper.WrapperSimpleApp;

/**
 * Determine which is the main class to run and delegate control to the
 * Java Service Wrapper.
 *
 * @author Travis Carlson
 */
public class MuleBootstrap {

    public static void main(String args[]) {

        // See if a main class was given, otherwise use MuleServer.
        String mainClassName = SystemUtils.getCommandLineOption("-main", args);
        if (mainClassName == null) {
            mainClassName = MuleServer.class.getName();
        }

        // Add the main class name as the first argument to the Wrapper.
        String[] appArgs = new String[args.length + 1];
        appArgs[0] = mainClassName;
        System.arraycopy(args, 0, appArgs, 1, args.length);

        // Call the wrapper
        WrapperSimpleApp.main(appArgs);
    }
}
