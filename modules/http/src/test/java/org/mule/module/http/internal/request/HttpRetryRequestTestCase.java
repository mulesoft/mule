/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.internal.request.DefaultHttpRequester.DEFAULT_RETRY_ATTEMPTS;
import static org.mule.module.http.internal.request.DefaultHttpRequester.REMOTELY_CLOSED;
import static org.mule.module.http.internal.request.DefaultHttpRequester.RETRY_ATTEMPTS_PROPERTY;
import org.mule.api.MuleEvent;
import org.mule.module.http.utils.TestServerSocket;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.matchers.ThrowableMessageMatcher;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


@RunWith(Parameterized.class)
public class HttpRetryRequestTestCase extends FunctionalTestCase
{

    private static ThrowableMessageMatcher REMOTELY_CLOSE_CAUSE_MATCHER = new ThrowableMessageMatcher<>(containsString(REMOTELY_CLOSED));

    @Rule
    public DynamicPort port = new DynamicPort("httpPort");

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public SystemProperty retryAttemptsSystemProperty;

    private int retryAttempts;

    @Parameters
    public static Object[] data()
    {
        int notRetryRequests = 0;
        int customRetryRequests = 2;
        return new Object[] {notRetryRequests, customRetryRequests, DEFAULT_RETRY_ATTEMPTS};
    }

    @Override
    protected String getConfigFile()
    {
        return "http-retry-policy-config.xml";
    }


    public HttpRetryRequestTestCase(Integer retryAttempts)
    {
        retryAttemptsSystemProperty = new SystemProperty(RETRY_ATTEMPTS_PROPERTY, retryAttempts.toString());
        this.retryAttempts = retryAttempts;
    }

    @Test
    public void nonIdempotentMethod() throws Exception
    {
        runRetryPolicyTest("POST", 0);
    }

    @Test
    public void idempotentMethod() throws Exception
    {
        runRetryPolicyTest("GET", retryAttempts);
    }

    void runRetryPolicyTest(String httpMethod, int numberOfRetryExpected) throws Exception
    {
        TestServerSocket testServerSocket = new TestServerSocket(port.getNumber(), numberOfRetryExpected + 1);
        assertThat("Http server can't be initialized.", testServerSocket.startServer(5000), is(true));
        expectedException.expectCause(REMOTELY_CLOSE_CAUSE_MATCHER);
        try
        {
            MuleEvent event = getTestEvent(null);
            event.setFlowVariable("httpMethod", httpMethod);
            runFlow("testRetryPolicy", event);
        }
        finally
        {
            assertThat(testServerSocket.getConnectionCounter() - 1, is(numberOfRetryExpected));
            assertThat("There was an error trying to dispose the http server.", testServerSocket.dispose(5000), is(true));
        }
    }

}
