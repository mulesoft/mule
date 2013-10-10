/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
