/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.vm.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.PropertyScope;
import org.mule.module.client.MuleClient;
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

        MuleClient client = new MuleClient(muleContext);

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
