/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import org.mule.api.MuleEventContext;
import org.mule.api.routing.RoutingException;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.junit4.FunctionalTestCase;

import java.io.InputStream;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class UntilSuccessfulStreamCloserTestCase extends FunctionalTestCase
{

    @Rule
    public ExpectedException expectedException;

    private static final InputStream firstInputStream = mock(InputStream.class);
    private static final InputStream secondInputStream = mock(InputStream.class);
    private static final InputStream thirdInputStream = mock(InputStream.class);
    private static final InputStream[] streams = {firstInputStream, secondInputStream, thirdInputStream};
    private static CountDownLatch latch;
    private static int i = 0;
    private String flowPrefix;

    public UntilSuccessfulStreamCloserTestCase(String flowPrefix, ExpectedException expectedException)
    {
        this.flowPrefix = flowPrefix;
        this.expectedException = expectedException;
    }

    @Parameters
    public static Collection<Object[]> data()
    {
        return asList(new Object[][] {
                {"asynchronous", ExpectedException.none()},
                {"synchronous", getExpectedRoutingException()}
        });
    }

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/until-successful-stream-closer-config.xml";
    }

    @Before
    public void setUp()
    {
        reset(firstInputStream, secondInputStream, thirdInputStream);
        latch = new CountDownLatch(streams.length);
    }

    @Test
    public void retryPayloadsAreClosed() throws Exception
    {
        runFlow(flowPrefix + "UntilSuccessfulStreamCloser");
        assertThat("The failure processor was not executed.", latch.await(10000, MILLISECONDS), is(true));
        for (InputStream stream : streams)
        {
            verify(stream).close();
        }
    }

    private static ExpectedException getExpectedRoutingException()
    {
        ExpectedException routingExpectedException = ExpectedException.none();
        routingExpectedException.expect(RoutingException.class);
        return routingExpectedException;
    }

    public static class PayloadSetter implements EventCallback
    {

        @Override
        public void eventReceived(MuleEventContext context, Object component) throws Exception
        {
            context.getMessage().setPayload(streams[i++]);
            latch.countDown();
            throw new RuntimeException("Failure exception");
        }
    }

}
