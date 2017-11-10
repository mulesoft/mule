/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.mule.module.http.internal.request.DefaultHttpRequester.RETRY_ATTEMPTS_PROPERTY;
import static org.junit.runners.Parameterized.Parameters;

import org.mule.tck.junit4.rule.SystemProperty;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HttpRetryRequestAttemptsTestCase extends AbstractHttpRetryRequestTestCase
{

    @Rule
    public SystemProperty retryAttemptsProperty;

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
        this.retryAttemptsProperty = new SystemProperty(RETRY_ATTEMPTS_PROPERTY, numberOfRetries.toString());
        this.numberOfRetries = numberOfRetries;
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
