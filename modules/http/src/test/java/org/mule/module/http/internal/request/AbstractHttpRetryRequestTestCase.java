/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request;

import static java.lang.reflect.Modifier.FINAL;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.internal.request.DefaultHttpRequester.DEFAULT_RETRY_ATTEMPTS;
import static org.mule.module.http.internal.request.DefaultHttpRequester.REMOTELY_CLOSED;
import org.mule.module.http.utils.TestServerSocket;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Rule;
import org.junit.internal.matchers.ThrowableMessageMatcher;
import org.junit.rules.ExpectedException;

public abstract class AbstractHttpRetryRequestTestCase extends FunctionalTestCase
{

    private static ThrowableMessageMatcher REMOTELY_CLOSE_CAUSE_MATCHER = new ThrowableMessageMatcher<>(containsString(REMOTELY_CLOSED));

    @Rule
    public DynamicPort port = new DynamicPort("httpPort");

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
    protected String getConfigFile()
    {
        return "http-retry-policy-config.xml";
    }

    @Before
    public void setUp() throws Exception
    {
        expectedException.expectCause(REMOTELY_CLOSE_CAUSE_MATCHER);
        Field retryAttemptsField = DefaultHttpRequester.class.getDeclaredField("RETRY_ATTEMPTS");
        retryAttemptsField.setAccessible(true);
        retryAttemptsField.setInt(null, getNumberOfRetries());
    }

    void runIdempotentFlow() throws Exception
    {
        runIdempotentFlow(DEFAULT_RETRY_ATTEMPTS);
    }

    void runIdempotentFlow(int numberOfRetryExpected) throws Exception
    {
        TestServerSocket testServerSocket = new TestServerSocket(port.getNumber(), numberOfRetryExpected + 1);
        assertThat("Http server can't be initialized.", testServerSocket.startServer(5000), is(true));
        try
        {
            runFlow("retryIdempotentMethod");
        }
        finally
        {
            assertThat(testServerSocket.getConnectionCounter() - 1, is(numberOfRetryExpected));
        }
        assertThat("There was an error trying to dispose the http server.", testServerSocket.dispose(5000), is(true));
    }

    void runNonIdempotentFlow() throws Exception
    {
        TestServerSocket testServerSocket = new TestServerSocket(port.getNumber(), 1);
        assertThat("Http server can't be initialized.", testServerSocket.startServer(5000), is(true));
        try
        {
            runFlow("retryNonIdempotentMethod");
        }
        finally
        {
            assertThat(testServerSocket.getConnectionCounter() - 1, is(0));
        }
        assertThat("There was an error trying to dispose the http server.", testServerSocket.dispose(5000), is(true));
    }

    protected int getNumberOfRetries ()
    {
        return DEFAULT_RETRY_ATTEMPTS;
    }

}
