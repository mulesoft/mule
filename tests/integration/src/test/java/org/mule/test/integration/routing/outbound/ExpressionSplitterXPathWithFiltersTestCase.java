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
import org.mule.api.MuleException;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import java.util.List;

import org.custommonkey.xmlunit.XMLUnit;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

public class ExpressionSplitterXPathWithFiltersTestCase extends FunctionalTestCase
{
    MuleClient client;

    private final String MESSAGE = "<Foo>\n" +
            "    <Bar>\n" +
            "        <One>One</One>\n" +
            "        <Three>Three</Three>\n" +
            "        <Two>Two</Two>\n" +
            "        <Three>Three</Three>\n" +
            "        <Three>Three</Three>\n" +
            "        <One>One</One>\n" +
            "    </Bar>    \n" +
            "</Foo>";



    public ExpressionSplitterXPathWithFiltersTestCase()
    {
        XMLUnit.setIgnoreWhitespace(true);
    }

    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/outbound/expression-splitter-xpath-with-filters-test.xml";
    }

    public void testRecipientList() throws Exception
    {
        client = new MuleClient();
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