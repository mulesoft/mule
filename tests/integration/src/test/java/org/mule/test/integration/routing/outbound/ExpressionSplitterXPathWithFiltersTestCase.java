/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.routing.outbound;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/outbound/expression-splitter-xpath-with-filters-test.xml";
    }

    @Test
    public void testRecipientList() throws Exception
    {
        client = new MuleClient(muleContext);
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
