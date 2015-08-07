/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Ignore;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;

@Ignore("MULE-4483")
public class ExpressionSplitterXPathWithFiltersTestCase extends FunctionalTestCase
{
    private static final String MESSAGE = "<Foo>\n" +
            "    <Bar>\n" +
            "        <One>One</One>\n" +
            "        <Three>Three</Three>\n" +
            "        <Two>Two</Two>\n" +
            "        <Three>Three</Three>\n" +
            "        <Three>Three</Three>\n" +
            "        <One>One</One>\n" +
            "    </Bar>    \n" +
            "</Foo>";

    private MuleClient client;

    public ExpressionSplitterXPathWithFiltersTestCase()
    {
        XMLUnit.setIgnoreWhitespace(true);
    }

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/outbound/expression-splitter-xpath-with-filters-test.xml";
    }

    @Test
    public void testRecipientList() throws Exception
    {
        client = muleContext.getClient();
        client.dispatch("vm://distributor.queue", MESSAGE, null);

        readFromQueue("vm://service1.out", 2, "One");
        readFromQueue("vm://service2.out", 1, "Two");
        readFromQueue("vm://service3.out", 3, "Three");
    }

    public void readFromQueue(String name, int expectedNumber, String number) throws Exception
    {
        MuleMessage message;
        for (int i = 0; i < expectedNumber; i++)
        {
            message = client.request(name, 2000L);
            assertNotNull(message);
            XMLUnit.compareXML("<" + number + ">" + number + "</" + number + ">", message.getPayloadAsString());
        }

        assertNull(client.request(name, 1000L));
    }
}
