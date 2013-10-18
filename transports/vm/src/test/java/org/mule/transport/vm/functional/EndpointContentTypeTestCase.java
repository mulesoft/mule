/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.vm.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MessagingException;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class EndpointContentTypeTestCase extends AbstractServiceAndFlowTestCase
{
    public EndpointContentTypeTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/config/content-type-setting-endpoint-configs-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/config/content-type-setting-endpoint-configs-flow.xml"}
        });
    }      
    
    @Test
    public void testContentTypes() throws Exception
    {
        MuleMessage response;
        Map<String, Object> messageProperties = new HashMap<String, Object>();
        messageProperties.put("content-type", "text/xml");
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("vm://in1?connector=vm-in1", "<OK/>", messageProperties);
        assertNotNull(result.getExceptionPayload());
        assertTrue(result.getExceptionPayload().getException() instanceof MessagingException);

        messageProperties.put("content-type", "text/plain");
        EchoComponent.setExpectedContentType("text/plain");
        response = client.send("vm://in1?connector=vm-in1", "OK", messageProperties);
        assertNotNull(response);
        assertEquals("OK", response.getPayload());

        messageProperties.remove("content-type");
        EchoComponent.setExpectedContentType("text/plain");
        response = client.send("vm://in1?connector=vm-in1", "OK", messageProperties);
        assertNotNull(response);
        assertEquals("OK", response.getPayload());

        messageProperties.put("content-type", "text/plain");
        EchoComponent.setExpectedContentType("text/xml");
        response = client.send("vm://in2?connector=vm-in2", "OK", messageProperties);
        assertNotNull(response);
        assertEquals("OK", response.getPayload());
    }

    public static class EchoComponent implements Callable
    {
        static String expectedContentType;

        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            MuleMessage message = eventContext.getMessage();
            assertEquals(expectedContentType, message.getProperty("content-type", PropertyScope.INBOUND));
            return message;
        }

        public static void setExpectedContentType(String expectedContentType)
        {
            EchoComponent.expectedContentType = expectedContentType;
        }
    }
}
    