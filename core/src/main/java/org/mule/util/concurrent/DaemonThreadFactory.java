/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.concurrent;

import edu.emory.mathcs.backport.java.util.concurrent.ThreadFactory;

public class DaemonThreadFactory extends NamedThreadFactory implements ThreadFactory
{

    public DaemonThreadFactory(String name)
    {
        this(name, Thread.NORM_PRIORITY);
    }

    public DaemonThreadFactory(String name, int priority)
    {
        super(name, priority);
    }

    public Thread newThread(Runnable runnable)
    {
        Thread t = super.newThread(runnable);
        t.setDaemon(true);
        return t;
    }

}
