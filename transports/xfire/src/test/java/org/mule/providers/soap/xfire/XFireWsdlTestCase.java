/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire;

import org.mule.extras.client.MuleClient;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOMessage;

import org.custommonkey.xmlunit.XMLAssert;

public class XFireWsdlTestCase extends AbstractMuleTestCase
{
    public static final String STOCKQUOTE_URL = "wsdl-xfire:http://www.webservicex.net/stockquote.asmx?WSDL&method=GetQuote";
    public static final String STOCK_SYMBOL = "AAPL";

    public void testXFireWsdlService() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage reply = client.send(STOCKQUOTE_URL, STOCK_SYMBOL, null);
        assertNotNull(reply);

        String payload = reply.getPayloadAsString();
        assertNotNull(payload);
        XMLAssert.assertXpathEvaluatesTo(STOCK_SYMBOL, "//StockQuotes/Stock/Symbol", payload);
    }

}
