/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.logging;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 *  A utility class cleaning up the logger repositories once the classloader for the application went
 *  out of scope and became eligible fot GC (e.g. app redeployed or undeployed).
 */
public class LoggerReferenceHandler
{

    // note that this is a direct log4j logger declaration, not a clogging one
    protected org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(getClass());

    public LoggerReferenceHandler(final String threadName,
                                  final ReferenceQueue<ClassLoader> referenceQueue,
                                  final Map<PhantomReference<ClassLoader>, Integer> references,
                                  final Map<Integer, ?> loggerRepository)
    {
        Executors.newSingleThreadExecutor(new ThreadFactory()
        {
            public Thread newThread(Runnable r)
            {
                final Thread thread = Executors.defaultThreadFactory().newThread(r);
                thread.setName(threadName);
                // CRITICAL - do NOT try to set this thread as a daemon or lower its priority,
                // as it may never get scheduled by JVM then, really tricky
                return thread;
            }
        }).submit(new Runnable()
        {
            public void run()
            {
                while (!Thread.currentThread().isInterrupted())
                {
                    try
                    {
                        final Reference<? extends ClassLoader> reference = referenceQueue.remove();
                        logger.debug("Cleaning up the logging hierarchy");
                        final Integer classLoaderHash = references.get(reference);
                        loggerRepository.remove(classLoaderHash);
                        reference.clear();
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                    }
                    catch (RuntimeException rex)
                    {
                        logger.error("Zombies detected, run for your life", rex);
                    }
                }
            }
        });
    }
}
