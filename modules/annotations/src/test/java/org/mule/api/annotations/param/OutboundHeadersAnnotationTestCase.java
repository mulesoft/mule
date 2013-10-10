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
