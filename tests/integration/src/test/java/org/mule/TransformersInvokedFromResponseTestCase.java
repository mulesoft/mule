/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transformer.AbstractTransformer;

import org.junit.Test;

public class TransformersInvokedFromResponseTestCase extends FunctionalTestCase
{

    private static int counter1 = 0;

    @Override
    protected String getConfigResources()
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
