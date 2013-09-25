/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.probe.thread;

import org.mule.tck.probe.Probe;

import java.util.Set;

/**
* Checks if a thread with he given name exists
*/
public class ThreadExists implements Probe
{

    private final String logHandlerThreadName;

    public ThreadExists(String logHandlerThreadName)
    {
        this.logHandlerThreadName = logHandlerThreadName;
    }

    public boolean isSatisfied()
    {
        final Set<Thread> threadSet = Thread.getAllStackTraces().keySet();

        for (final Thread th : threadSet)
        {
            if (th.getName().equals(logHandlerThreadName))
            {
                return true;
            }
        }

        return false;
    }

    public String describeFailure()
    {
        return String.format("Expected to find a thread named '%s'", logHandlerThreadName);
    }
}
