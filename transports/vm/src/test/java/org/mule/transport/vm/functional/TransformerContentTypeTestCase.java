/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.vm.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

import javax.activation.MimeType;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class TransformerContentTypeTestCase extends AbstractServiceAndFlowTestCase
{
    public TransformerContentTypeTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/config/content-type-setting-transform-configs-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/config/content-type-setting-transform-configs-flow.xml"}
        });
    }

    @Test
    public void testContentTypes() throws Exception
    {
        MuleMessage response;
        Map<String, Object> messageProperties = new HashMap<String, Object>();

        MuleClient client = muleContext.getClient();

        messageProperties.put("content-type", "text/plain");
        EchoComponent.setExpectedMimeType("text/xml");
        response = client.send("vm://in1?connector=vm-in1", "OK", messageProperties);
        assertNotNull(response);
        assertEquals("OK", response.getPayload());

        messageProperties.remove("content-type");
        response = client.send("vm://in1?connector=vm-in1", "OK", messageProperties);
        assertNotNull(response);
        assertEquals("OK", response.getPayload());

        messageProperties.put("content-type", "text/xml");
        EchoComponent.setExpectedMimeType("text/plain");
        response = client.send("vm://in2?connector=vm-in2", "OK", messageProperties);
        assertNotNull(response);
        assertEquals("OK", response.getPayload());

        messageProperties.remove("content-type");
        response = client.send("vm://in2?connector=vm-in2", "OK", messageProperties);
        assertNotNull(response);
        assertEquals("OK", response.getPayload());
    }

    public static class EchoComponent implements Callable
    {
        static String expectedMimeType;

        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            MuleMessage message = eventContext.getMessage();
            String contentType = message.getProperty("content-type", PropertyScope.INBOUND);
            MimeType mt = new MimeType(contentType);
            String mimeType = mt.getPrimaryType() + "/" + mt.getSubType();
            assertEquals(expectedMimeType, mimeType);
            return message;
        }

        public static void setExpectedMimeType(String expectedContentType)
        {
            EchoComponent.expectedMimeType = expectedContentType;
        }
    }
}
