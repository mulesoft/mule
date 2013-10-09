/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
