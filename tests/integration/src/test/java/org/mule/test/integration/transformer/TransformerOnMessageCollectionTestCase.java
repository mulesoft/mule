/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

public class TransformerOnMessageCollectionTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/transformers/transformer-on-message-collection-config.xml";
    }

    @Test
    public void testIssue() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        List<String> values = new LinkedList<String>();
        values.add("One");
        values.add("Two");

        MuleMessage response = client.send("vm://testInput", values, null);
        assertEquals("foo", response.getPayload());
        assertFalse(response instanceof MuleMessageCollection);
    }
}
