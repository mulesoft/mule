/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.logging;

import org.mule.tck.probe.PollingProber;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
@SmallTest
public abstract class AbstractLogHandlerThreadTestCase
{

    protected static boolean createdLoggerReferenceHandler;

    protected final LoggerFactoryFactory loggerFactory;
    protected final String logHandlerThreadName;
    protected final PollingProber prober = new PollingProber(100, 10);

    public AbstractLogHandlerThreadTestCase(LoggerFactoryFactory loggerFactory, String logHandlerThreadName)
    {
        this.logHandlerThreadName = logHandlerThreadName;
        this.loggerFactory = loggerFactory;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {new MuleLoggerFactoryFactory(), MuleLoggerFactory.LOG_HANDLER_THREAD_NAME},
                {new MuleLogFactoryFactory(), MuleLogFactory.LOG_HANDLER_THREAD_NAME}
        });
    }

    @Before
    public void setUp() throws Exception
    {
        createdLoggerReferenceHandler = false;
    }

    public static interface LoggerFactoryFactory
    {

        Object create();
    }

    public static class MuleLoggerFactoryFactory implements LoggerFactoryFactory
    {

        public Object create()
        {
            return new MuleLoggerFactory()
            {

                @Override
                protected void createLoggerReferenceHandler()
                {
                    createdLoggerReferenceHandler = true;
                }
            };
        }
    }

    public static class MuleLogFactoryFactory implements LoggerFactoryFactory
    {

        public Object create()
        {
            return new MuleLogFactory()
            {
                @Override
                protected void createLoggerReferenceHandler()
                {
                    createdLoggerReferenceHandler = true;
                }
            };
        }
    }
}
