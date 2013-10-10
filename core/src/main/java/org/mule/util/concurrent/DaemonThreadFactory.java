/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.concurrent;

public class DaemonThreadFactory extends NamedThreadFactory
{

    public DaemonThreadFactory(String name)
    {
        super(name);
    }

    public DaemonThreadFactory(String name, ClassLoader contextClassLoader)
    {
        super(name, contextClassLoader);
    }

    @Override
    protected void doConfigureThread(Thread t)
    {
        t.setDaemon(true);
    }

}
