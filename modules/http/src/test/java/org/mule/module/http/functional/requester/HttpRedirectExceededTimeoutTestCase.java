/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeoutException;

import org.junit.Test;

public class HttpRedirectExceededTimeoutTestCase extends AbstractHttpRedirectTimeoutTestCase
{

    private static long TIMEOUT = 600;

    public HttpRedirectExceededTimeoutTestCase()
    {
        super(TIMEOUT, TIMEOUT * 2);
    }

    @Test
    public void testRedirectTimeout() throws Exception
    {
        try
        {
            runFlow("requestFlow");
            fail("Timeout exception must be triggered");
        }
        catch(Exception timeoutException)
        {
            assertThat(timeoutException.getCause(), instanceOf(TimeoutException.class));
        }
    }


}
