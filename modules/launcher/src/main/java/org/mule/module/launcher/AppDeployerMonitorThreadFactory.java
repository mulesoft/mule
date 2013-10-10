/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.launcher;

import org.mule.util.concurrent.LoggingUncaughtExceptionHandler;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class AppDeployerMonitorThreadFactory implements ThreadFactory
{

    static final AtomicInteger poolNumber = new AtomicInteger(1);
    final ThreadGroup group;
    final AtomicInteger threadNumber = new AtomicInteger(1);
    final String namePrefix;

    public AppDeployerMonitorThreadFactory()
    {
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() :
                Thread.currentThread().getThreadGroup();
        namePrefix = String.format("Mule.app.deployer.monitor.%d.thread.",  poolNumber.getAndIncrement());
    }

    public Thread newThread(Runnable r)
    {
        Thread t = new Thread(group, r,
                              namePrefix + threadNumber.getAndIncrement(),
                              0);
        // make sure it's non-daemon, allows for an 'idle' state of Mule by preventing early termination
        t.setDaemon(false);
        t.setPriority(Thread.MIN_PRIORITY);
        t.setUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler());
        return t;
    }


}
