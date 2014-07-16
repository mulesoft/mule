/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.param;

import org.mule.api.model.InvocationResult;

import java.util.Map;

import javax.activation.DataHandler;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OutboundAttachmentsAnnotationTestCase extends AbstractAnnotatedEntrypointResolverTestCase
{
    @Override
    public void doSetUp() throws Exception
    {
        inboundScope = false;
        super.doSetUp();
    }

    @Override
    protected Object getComponent()
    {
        return new OutboundAttachmentsAnnotationComponent();

    }

    @Test
    public void testProcessAttachment() throws Exception
    {
        InvocationResult response = invokeResolver("processAttachments", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<String, DataHandler> result = (Map<String, DataHandler>)response.getResult();
        assertEquals("barValue", readAttachment(result.get("bar")));
    }

    @Test
    public void testProcessAttachmentWithExistingOutAttachments() throws Exception
    {
        InvocationResult response = invokeResolver("processAttachments", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<String, DataHandler> result = (Map<String, DataHandler>)response.getResult();
        assertEquals("barValue", readAttachment(result.get("bar")));
        assertEquals("fooValue", readAttachment(result.get("foo")));
    }

    @Test
    public void testInvalidParamType() throws Exception
    {
        try
        {
            invokeResolver("invalidParamType", eventContext);
            fail("Should not be able to resolve invalid attachment type");
        }
        catch (Exception e)
        {
            //Expected
        }
    }
}
