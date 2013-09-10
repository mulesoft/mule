/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.lang.time.StopWatch;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class HttpContinueFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    /**
     * HttpClient has default 3 seconds wait for Expect-Continue calls.
     */
    private static final int DEFAULT_HTTP_CLIENT_CONTINUE_WAIT = 3000;

    protected StopWatch stopWatch;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public HttpContinueFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "http-functional-test-service.xml"},
            {ConfigVariant.FLOW, "http-functional-test-flow.xml"}
        });
    }

    @Test
    public void testSendWithContinue() throws Exception
    {
        stopWatch = new StopWatch();
        MuleClient client = muleContext.getClient();

        //Need to use Http1.1 for Expect: Continue
        HttpClientParams params = new HttpClientParams();
        params.setVersion(HttpVersion.HTTP_1_1);
        params.setBooleanParameter(HttpClientParams.USE_EXPECT_CONTINUE, true);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(HttpConnector.HTTP_PARAMS_PROPERTY, params);

        stopWatch.start();
        MuleMessage result = client.send("clientEndpoint", TEST_MESSAGE, props);
        stopWatch.stop();

        assertNotNull(result);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());

        if (stopWatch.getTime() > DEFAULT_HTTP_CLIENT_CONTINUE_WAIT)
        {
            fail("Server did not handle Expect=100-continue header properly,");
        }
    }
}
