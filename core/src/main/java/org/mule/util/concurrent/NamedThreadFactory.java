/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.concurrent;

import org.mule.util.StringUtils;

import java.util.concurrent.atomic.AtomicLong;

public class NamedThreadFactory implements java.util.concurrent.ThreadFactory
{
    private final String name;
    private final AtomicLong counter;
    private final ClassLoader contextClassLoader;

    public NamedThreadFactory(String name)
    {
        this(name, null);
    }

    public NamedThreadFactory(String name, ClassLoader contextClassLoader)
    {
        if (StringUtils.isEmpty(name))
        {
            throw new IllegalArgumentException("NamedThreadFactory must have a proper name.");
        }

        this.name = name;
        this.contextClassLoader = contextClassLoader;
        this.counter = new AtomicLong(1);
    }

    public Thread newThread(Runnable runnable)
    {
        Thread t = new Thread(runnable);
        configureThread(t);
        return t;
    }

    protected void configureThread(Thread t)
    {
        if (contextClassLoader != null)
        {
            t.setContextClassLoader(contextClassLoader);
        }
        doConfigureThread(t);
    }

    protected void doConfigureThread(Thread t)
    {
        t.setName(String.format("%s.%02d", name, counter.getAndIncrement()));
    }

    public ClassLoader getContextClassLoader()
    {
        return contextClassLoader;
    }

    public AtomicLong getCounter()
    {
        return counter;
    }

    public String getName()
    {
        return name;
    }
}
