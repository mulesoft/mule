/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.RequestContext;
import org.mule.transformer.AbstractTransformer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TransformerStoppingEventFlowTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/transformers/transformer-stopped-processing.xml";
    }

    @Test
    public void testNullReturnStopsFlow() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage msg = client.send("vm://in", TEST_MESSAGE, null);
        assertNotNull(msg);
        final String payload = msg.getPayloadAsString();
        assertNotNull(payload);
        assertEquals(TEST_MESSAGE, payload);
    }

    public static final class StopFlowTransformer extends AbstractTransformer
    {


        protected Object doTransform(Object src, String encoding) throws TransformerException
        {
            RequestContext.getEventContext().setStopFurtherProcessing(true);
            return src;
        }
    }
}
