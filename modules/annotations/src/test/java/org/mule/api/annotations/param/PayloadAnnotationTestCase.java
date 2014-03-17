/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
            fail("There si no transformer registered for converting String to StringBuilder");
        }
        catch (Exception e)
        {
            //expected
            assertTrue(e.getCause() instanceof TransformerException);
        }
    }
}
