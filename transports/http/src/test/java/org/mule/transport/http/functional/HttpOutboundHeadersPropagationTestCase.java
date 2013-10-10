/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

public class HttpOutboundHeadersPropagationTestCase extends HttpFunctionalTestCase
{
    protected static String TEST_MESSAGE = "Test Http Request (R�dgr�d), 57 = \u06f7\u06f5 in Arabic";
    private static String TEST_JAPANESE_MESSAGE = "\u3042";

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

        MuleMessage reply = client.request("vm://out", 120000);
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


