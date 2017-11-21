/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request;

import static java.lang.reflect.Modifier.FINAL;
import static org.junit.runners.Parameterized.Parameters;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HttpRetryRequestAttemptsTestCase extends AbstractHttpRetryRequestTestCase
{

    private int numberOfRetries;

    @Parameters
    public static Object[] data()
    {
        int notRetryRequests = 0;
        int customRetryRequests = 2;
        return new Object[] {notRetryRequests, customRetryRequests};
    }

    public HttpRetryRequestAttemptsTestCase(Integer numberOfRetries)
    {
        this.numberOfRetries = numberOfRetries;
    }

    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        Field retryAttemptsField = DefaultHttpRequester.class.getDeclaredField("RETRY_ATTEMPTS");
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(retryAttemptsField, retryAttemptsField.getModifiers() & ~FINAL);
        retryAttemptsField.setAccessible(true);
        retryAttemptsField.setInt(null, numberOfRetries);
    }

    @Test
    public void customRetryRequestAttemptsIdempotentMethod() throws Exception
    {
        runIdempotentFlow(numberOfRetries);
    }

    @Test
    public void customRetryRequestAttemptsNonIdempotentMethod() throws Exception
    {
        runNonIdempotentFlow();
    }

}
