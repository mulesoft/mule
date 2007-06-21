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

import org.mule.util.StringUtils;

import edu.emory.mathcs.backport.java.util.concurrent.ThreadFactory;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicLong;

public class NamedThreadFactory implements ThreadFactory
{
    private final String name;
    private final AtomicLong counter;

    public NamedThreadFactory(String name)
    {
        if (StringUtils.isEmpty(name))
        {
            throw new IllegalArgumentException("NamedThreadFactory must have a proper name.");
        }

        this.name = name;
        this.counter = new AtomicLong(1);
    }

    public Thread newThread(Runnable runnable)
    {
        Thread t = new Thread(runnable, name + '.' + counter.getAndIncrement());
        return t;
    }

}
