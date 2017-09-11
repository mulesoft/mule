/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static com.ning.http.client.AsyncHttpClientConfigDefaults.ASYNC_CLIENT;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleEvent;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

public class HttpRedirectTimeoutTestCase extends AbstractHttpRedirectTimeoutTestCase
{
    private static String GLOBAL_TIMEOUT = "300";

    private static String GLOBAL_REQUEST_TIMEOUT = ASYNC_CLIENT + "requestTimeout";

    private static long TIMEOUT = 600;

    private static long DELAY = 450;

    @Rule
    public SystemProperty globalRequestTimeoutSystemProperty = new SystemProperty(GLOBAL_REQUEST_TIMEOUT, GLOBAL_TIMEOUT);

    public HttpRedirectTimeoutTestCase()
    {
        super(TIMEOUT, DELAY);
    }

    @Test
    public void testRedirectTimeout() throws Exception
    {
        MuleEvent event = runFlow("requestFlow");
        assertThat(event.getMessage().getPayloadAsString(), is("OK"));
    }

}
