/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction.xa;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;

import java.util.concurrent.atomic.AtomicInteger;

public class FailureGeneratorMessageProcessor implements MessageProcessor
{

    private static int maximumNumberOfFailures = 5;
    private static double failureRange = 0;
    private AtomicInteger numberOfFailures = new AtomicInteger();

    public static void generateIntermitentFailure()
    {
        failureRange = 0.5;
        resetMaximumNumberOfFailures();
    }

    private static void resetMaximumNumberOfFailures()
    {
        maximumNumberOfFailures = 5;
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (numberOfFailures.get() < maximumNumberOfFailures)
        {
            if (Math.random() < failureRange)
            {
                numberOfFailures.incrementAndGet();
                throw new RuntimeException("Random exception thrown on purpose.");
            }
        }
        return event;
    }

    public static void noFailure()
    {
        failureRange = -1;
        resetMaximumNumberOfFailures();
    }

    public static void allFailure()
    {
        failureRange = 1;
        maximumNumberOfFailures = Integer.MAX_VALUE;
    }
}
