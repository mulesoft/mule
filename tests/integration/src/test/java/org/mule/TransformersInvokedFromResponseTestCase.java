/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.transformer.TransformerException;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.transformer.AbstractTransformer;

import org.junit.Test;

public class TransformersInvokedFromResponseTestCase extends FunctionalTestCase
{
    private static int counter1 = 0;

    @Override
    protected String getConfigFile()
    {
        return "transformers-invoked-from-response-config.xml";
    }

    @Test
    public void testTransformersAreCorrectlyInvoked() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage test = client.send("jms://testQueue", "TEST1", null);
        assertNotNull(test);
        assertEquals(1, counter1);
        assertEquals("TEST1 transformed", test.getPayload());

        test = client.send("jms://testQueue", "TEST2", null);
        assertNotNull(test);
        assertEquals(2, counter1);
        assertEquals("TEST2 transformed", test.getPayload());
    }

    public static class InvocationCounterTransformer1 extends AbstractTransformer
    {
        @Override
        protected Object doTransform(Object src, String enc) throws TransformerException
        {
            counter1++;
            return src;
        }
    }
}
