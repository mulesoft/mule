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
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.MuleSession;
import org.mule.providers.AbstractConnector;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;

import org.custommonkey.xmlunit.XMLAssert;

public class XFireWsdlTestCase extends AbstractMuleTestCase
{
    public static final String STOCKQUOTE_URL = "wsdl-xfire:http://www.webservicex.net/stockquote.asmx?WSDL&method=GetQuote";
    public static final String STOCKQUOTE_URL_NOWSDL = "wsdl-xfire:http://www.webservicex.net/stockquote.asmx?method=GetQuote";
    public static final String STOCKQUOTE_URL_WSDL = "http://www.webservicex.net/stockquote.asmx?WSDL";
    public static final String STOCK_SYMBOL = "AAPL";

    public void testXFireWsdlService() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage reply = client.send(STOCKQUOTE_URL, STOCK_SYMBOL, null);
        assertNotNull(reply);

        String response = reply.getPayloadAsString();
        assertNotNull(response);
        XMLAssert.assertXpathEvaluatesTo(STOCK_SYMBOL, "//StockQuotes/Stock/Symbol", response);
    }

    /**
     * This tests the endpoint propery of wsdlUrl which specifies an
     * alternative WSDL location (see MULE-1368)
     */
    public void testXFireWsdlServiceWithEndpointParam() throws Exception
    {
        UMOEndpoint endpoint = managementContext.getRegistry().getOrCreateEndpointForUri(STOCKQUOTE_URL_NOWSDL, UMOEndpoint.ENDPOINT_TYPE_SENDER);
        endpoint.setProperty("wsdlUrl", STOCKQUOTE_URL_WSDL);

        UMOMessage message = new MuleMessage(STOCK_SYMBOL);
        UMOSession session = new MuleSession(message,
                ((AbstractConnector) endpoint.getConnector())
                        .getSessionHandler());
        MuleEvent event = new MuleEvent(message, endpoint, session, true);
        UMOMessage reply = session.sendEvent(event);

        assertNotNull(reply);

        String response = reply.getPayloadAsString();
        assertNotNull(response);

        XMLAssert.assertXpathEvaluatesTo(STOCK_SYMBOL, "//StockQuotes/Stock/Symbol", response);

    }

}
