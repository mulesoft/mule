/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEventContext;
import org.mule.api.exception.RollbackSourceCallback;
import org.mule.api.exception.SystemExceptionHandler;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import java.util.ConcurrentModificationException;

import org.junit.Rule;
import org.junit.Test;

public class DefaultMuleEventCopyTestCase extends FunctionalTestCase
{

    public static final int EXPECTED_EXECUTIONS_COUNT = 4000;

    @Rule
    public SystemProperty executionsCount = new SystemProperty("test.executionsCount", String.valueOf(EXPECTED_EXECUTIONS_COUNT));

    @Override
    public String getConfigFile()
    {
        return "copy-mule-event.xml";
    }

    @Test
    public void test() throws Exception
    {
        final Counter executionCounter = registerExecutionCounter();
        final Counter exceptionCounter = registerExceptionCounter();

        muleContext.getClient().send("vm://mainQueue", "dummy", null);

        waitUntilExecutionFinishes(executionCounter);

        assertThat(exceptionCounter.get(), is(0));
    }

    private void waitUntilExecutionFinishes(final Counter executionCounter)
    {
        new PollingProber(10000, 100).check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return executionCounter.get() == EXPECTED_EXECUTIONS_COUNT;
            }

            @Override
            public String describeFailure()
            {
                return String.format("Should have %d executions, but there were only %d.", EXPECTED_EXECUTIONS_COUNT, executionCounter.get());
            }
        });
    }

    private Counter registerExecutionCounter() throws Exception
    {
        FunctionalTestComponent testComponent = getFunctionalTestComponent("one-way-flow");
        assertThat(testComponent, not(nullValue()));

        final Counter executionCounter = new Counter();
        EventCallback callback = new EventCallback()
        {
            @Override
            public void eventReceived(final MuleEventContext context, final Object component) throws Exception
            {
                executionCounter.add();
            }
        };
        testComponent.setEventCallback(callback);
        return executionCounter;
    }

    private Counter registerExceptionCounter()
    {
        final Counter exceptionCounter = new Counter();
        muleContext.setExceptionListener(new SystemExceptionHandler()
        {
            @Override
            public void handleException(Exception exception, RollbackSourceCallback rollbackMethod)
            {
                if (exception instanceof ConcurrentModificationException)
                {
                    exceptionCounter.add();
                }
            }

            @Override
            public void handleException(Exception exception)
            {
                handleException(exception, null);
            }
        });
        return exceptionCounter;
    }

    static class Counter
    {

        private int count;

        public synchronized void add()
        {
            count++;
        }

        public synchronized int get()
        {
            return count;
        }
    }

}
