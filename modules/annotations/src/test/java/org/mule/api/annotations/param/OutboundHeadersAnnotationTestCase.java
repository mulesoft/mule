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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OutboundHeadersAnnotationTestCase extends AbstractAnnotatedEntrypointResolverTestCase
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
        return new OutboundHeadersAnnotationComponent();
    }

    @Test
    public void testProcessHeader() throws Exception
    {
        InvocationResult response = invokeResolver("processHeaders", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) response.getResult();
        assertEquals("barValue", result.get("bar"));
    }

    @Test
    public void testProcessHeaderWithExistingOutHeaders() throws Exception
    {
        eventContext.getMessage().setOutboundProperty("foo", "changeme");
        InvocationResult response = invokeResolver("processHeaders", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) response.getResult();
        assertEquals("barValue", result.get("bar"));
        assertEquals("fooValue", result.get("foo"));
    }

    @Test
    public void testInvalidParamType() throws Exception
    {
        try
        {
            invokeResolver("invalidParamType", eventContext);
            fail("Should not be able to resolve invalid header type");
        }
        catch (Exception e)
        {
            //Expected
        }
    }
}
