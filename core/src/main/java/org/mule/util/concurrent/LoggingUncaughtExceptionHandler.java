/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.concurrent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LoggingUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler
{

    protected transient Log logger = LogFactory.getLog(getClass());

    public void uncaughtException(Thread thread, Throwable throwable)
    {
        logger.error(String.format("Uncaught exception in %s%n%n", thread), throwable);
    }
}
