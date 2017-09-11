/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.isA;

import java.util.concurrent.TimeoutException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class HttpRedirectExceededTimeoutTestCase extends AbstractHttpRedirectTimeoutTestCase
{

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static long TIMEOUT = 600;

    public HttpRedirectExceededTimeoutTestCase()
    {
        super(TIMEOUT, TIMEOUT * 2);
    }

    @Test
    public void testRedirectTimeout() throws Exception
    {
        expectedException.expectCause(isA(TimeoutException.class));
        expectedException.reportMissingExceptionWithMessage("Timeout exception must be triggered");
        runFlow("requestFlow");
    }

}
