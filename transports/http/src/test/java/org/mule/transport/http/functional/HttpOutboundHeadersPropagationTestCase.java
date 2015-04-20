/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class HttpOutboundHeadersPropagationTestCase extends HttpFunctionalTestCase
{
    protected static String TEST_MESSAGE = "Test Http Request (R�dgr�d), 57 = \u06f7\u06f5 in Arabic";

    public HttpOutboundHeadersPropagationTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        setDisposeContextPerClass(true);
    }  

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.FLOW, "http-outbound-headers-propagation-flow.xml"}
        });
    }      

    @Override
    public void testSend() throws Exception
    {
        // no operation
    }
    
    @Test
    public void outboundHttpContentTypeTest() throws Exception
    {
        MuleClient client = muleContext.getClient();
        Map<String,Object> msgProps = new HashMap<String,Object>();
        msgProps.put("custom-header", "value-custom-header");
        client.dispatch("vm://in", "HelloWorld!", msgProps);

        MuleMessage reply = client.request("vm://out", RECEIVE_TIMEOUT);
        Map<String, Object> headers = (Map<String, Object>) reply.getPayload();

        for (String header : HttpConstants.REQUEST_HEADER_NAMES.values())
        {
            // TODO: the Expect header should be sent on the request, it seems the apache commons HttpClient 3.1 has
            // a bug the flag HttpMethodParams.USE_EXPECT_CONTINUE is always false when invoking
            // org.apache.commons.httpclient.methods.ExpectContinueMethod.addRequestHeaders()

            if(!HttpConstants.HEADER_EXPECT.equals(header))         // TODO: This should be sent on the request,
            {
                if(HttpConstants.HEADER_COOKIE.equals(header))
                {
                    assertNotNull("Request header <" + header + "> mshould be defined.", headers.get(HttpConnector.HTTP_COOKIES_PROPERTY));
                } else {
                    assertNotNull("Request header <" + header + "> should be defined.", headers.get(header));                
                }
            }

        }
        for (String header : HttpConstants.GENERAL_AND_ENTITY_HEADER_NAMES.values())
        {
            assertNotNull("General or Entity header <" + header + "> should be defined.", headers.get(header));
        }
        for (String header : HttpConstants.RESPONSE_HEADER_NAMES.values())
        {
            assertNull("Response header <" + header +"> should not be defined.", headers.get(header));
        }
        assertNotNull(reply);
    }
}


