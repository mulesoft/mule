/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleMessage;

import java.util.List;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;

public class ExpressionSplitterXPathTestCase extends AbstractIntegrationTestCase
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

    private final String EXPECTED_MESSAGE_1 = "<Trade xmlns=\"http://acme.com\">\n" +
            "        <Type>CASH</Type>\n" +
            "        <Amount>40000</Amount>\n" +
            "        <Currency>USD</Currency>\n" +
            "        <Date>28102008</Date>\n" +
            "        <Received/>\n" +
            "    </Trade>";

    private final String EXPECTED_MESSAGE_2 = "<Trade xmlns=\"http://acme.com\">\n" +
            "        <Type>CASH</Type>\n" +
            "        <Amount>2000</Amount>\n" +
            "        <Currency>GBP</Currency>\n" +
            "        <Date>28102008</Date>\n" +
            "        <Received/>\n" +
            "    </Trade>";

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/outbound/expression-splitter-xpath-test-flow-el.xml";
    }

    public ExpressionSplitterXPathTestCase()
    {
        XMLUnit.setIgnoreWhitespace(true);
    }

    @Test
    public void testRecipientList() throws Exception
    {
        MuleMessage result = flowRunner("Distributor").withPayload(MESSAGE).run().getMessage();

        assertNotNull(result);
        assertTrue(result.getPayload() instanceof List);
        List<String> results = ((List<MuleMessage>) result.getPayload()).stream().map(msg -> (String) msg.getPayload
                ()).collect(toList());
        assertEquals(2, results.size());

        assertTrue(XMLUnit.compareXML(EXPECTED_MESSAGE_1, results.get(0).toString()).identical());
        assertTrue(XMLUnit.compareXML(EXPECTED_MESSAGE_2, results.get(1).toString()).identical());
    }
}
