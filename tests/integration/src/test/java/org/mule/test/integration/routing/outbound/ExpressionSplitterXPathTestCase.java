/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.routing.outbound;

import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import java.util.List;

public class ExpressionSplitterXPathTestCase extends FunctionalTestCase
{
    private final String MESSAGE = "<Batch xmlns=\"http://acme.com\">\n" +
            "    <Trade>\n" +
            "        <Type>CASH</Type>\n" +
            "        <Amount>40000</Amount>\n" +
            "        <Currency>USD</Currency>\n" +
            "        <Date>28102008</Date>\n" +
            "    </Trade>    \n" +
            "    <Trade>\n" +
            "        <Type>CASH</Type>\n" +
            "        <Amount>2000</Amount>\n" +
            "        <Currency>GBP</Currency>\n" +
            "        <Date>28102008</Date>\n" +
            "    </Trade>    \n" +
            "</Batch>";

    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/outbound/expression-splitter-xpath-test.xml";
    }

    public void testRecipientList() throws Exception
    {


        MuleClient client = new MuleClient();
        MuleMessage result = client.send("vm://distributor.queue", MESSAGE, null);

        assertNotNull(result);
        assertTrue(result instanceof MuleMessageCollection);
        MuleMessageCollection coll = (MuleMessageCollection) result;
        assertEquals(2, coll.size());
        List results = (List) coll.getPayload();

        assertTrue(results.contains("DefaultDocument Received in ServiceOne"));
        assertTrue(results.contains("DefaultDocument Received in ServiceTwo"));
    }
}