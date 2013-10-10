/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.launcher;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A slightly tweaked default thread factory that uses the following pattern:
 * <code>[%s].config.change.%d.thread.%d</code>, where %s stands for application name,
 * the next number will tell one how many redeployments this app had during this container's
 * lifetime and the last digit, thread count, should always be 1. Left there for debugging
 * purposes to quickly locate any duplicate threads trying to perform a redeploy. 
 */
public class ConfigChangeMonitorThreadFactory implements ThreadFactory
{

    static final AtomicInteger poolNumber = new AtomicInteger(1);
    final ThreadGroup group;
    final AtomicInteger threadNumber = new AtomicInteger(1);
    final String namePrefix;
    final String appName;

    public ConfigChangeMonitorThreadFactory(String appName)
    {
        this.appName = appName;
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() :
                Thread.currentThread().getThreadGroup();
        namePrefix = String.format("[%s].config.change.%d.thread.", appName, poolNumber.getAndIncrement());
    }

    public Thread newThread(Runnable r)
    {
        Thread t = new Thread(group, r,
                              namePrefix + threadNumber.getAndIncrement(),
                              0);
        if (t.isDaemon())
        {
            t.setDaemon(false);
        }
        if (t.getPriority() != Thread.NORM_PRIORITY)
        {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
    }


}
