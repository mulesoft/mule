/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
