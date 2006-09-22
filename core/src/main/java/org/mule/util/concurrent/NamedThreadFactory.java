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
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.StringUtils;

public class NamedThreadFactory implements ThreadFactory
{
    private final String name;
    private final int priority;
    private final AtomicLong counter;

    public NamedThreadFactory(String name)
    {
        this(name, Thread.NORM_PRIORITY);
    }

    public NamedThreadFactory(String name, int priority)
    {
        if (StringUtils.isEmpty(name))
        {
            throw new IllegalArgumentException("NamedThreadFactory must have a proper name.");
        }

        this.name = name;
        this.priority = priority;
        this.counter = new AtomicLong(1);
    }

    public Thread newThread(Runnable runnable)
    {
        Thread t = new Thread(runnable, name + '.' + counter.getAndIncrement());
        t.setPriority(priority);
        return t;
    }

}
