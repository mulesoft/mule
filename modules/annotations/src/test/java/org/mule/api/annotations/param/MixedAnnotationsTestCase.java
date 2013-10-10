/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.annotations.param;

import org.mule.api.model.InvocationResult;

import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MixedAnnotationsTestCase extends AbstractAnnotatedEntrypointResolverTestCase
{
    @Override
    protected Object getComponent()
    {
        return new MixedAnnotationsComponent();
    }

    @Test
    public void testProcessAllAnnotated() throws Exception
    {
        InvocationResult response = invokeResolver("processAllAnnotated", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) response.getResult();
        assertEquals(3, result.size());

        //Payload
        assertEquals("test", result.get("payload"));

        //Headers
        assertNotNull(result.get("inboundHeaders"));
        Map<?, ?> headers = (Map<?, ?>)result.get("inboundHeaders");
        assertEquals(2, headers.size());
        assertEquals("fooValue", headers.get("foo"));
        assertEquals("barValue", headers.get("bar"));

        //Attachments
        assertNotNull(result.get("inboundAttachments"));
        Map<?, ?> attachments = (Map<?, ?>)result.get("inboundAttachments");
        assertEquals(3, attachments.size());
        assertNotNull(attachments.get("foo"));
        assertNotNull(attachments.get("bar"));
        assertNotNull(attachments.get("baz"));
    }

    @Test
    public void testPayloadNotAnnotated() throws Exception
    {
        //When using param annotations every param needs t obe annotated
        try
        {
            invokeResolver("processPayloadNotAnnotated", eventContext);
            fail("When using annotated parameters, all parameters must be anotated");
        }
        catch (IllegalArgumentException e)
        {
            assertEquals("wrong number of arguments", e.getMessage());
        }
    }
}
