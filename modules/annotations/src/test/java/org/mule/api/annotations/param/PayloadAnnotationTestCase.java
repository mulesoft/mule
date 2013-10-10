/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.annotations.param;

import org.mule.api.model.InvocationResult;
import org.mule.api.transformer.TransformerException;
import org.mule.util.IOUtils;

import java.io.InputStream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PayloadAnnotationTestCase extends AbstractAnnotatedEntrypointResolverTestCase
{
    @Override
    protected Object getComponent()
    {
        return new PayloadAnnotationComponent();
    }

    @Test
    public void testPayloadNoTransform() throws Exception
    {
       InvocationResult response = invokeResolver("processNoTransformString", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof String);
        assertEquals("test", response.getResult().toString());
    }

    @Test
    public void testPayloadAutoTransform() throws Exception
    {
        InvocationResult response = invokeResolver("processAutoTransformString", eventContext);
        assertTrue("Message payload should be a String", response.getResult() instanceof InputStream);
        assertEquals("test", IOUtils.toString((InputStream)response.getResult()));
    }

    @Test
    public void testPayloadFailedTransform() throws Exception
    {
        try
        {
            invokeResolver("processFailedAutoTransformString", eventContext);
            fail("There si no transformer registered for converting String to StringBuffer");
        }
        catch (Exception e)
        {
            //expected
            assertTrue(e.getCause() instanceof TransformerException);
        }
    }
}
