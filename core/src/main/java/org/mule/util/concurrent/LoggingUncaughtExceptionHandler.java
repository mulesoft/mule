/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
