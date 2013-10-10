/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
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
        MuleClient client = new MuleClient(muleContext);
        
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
