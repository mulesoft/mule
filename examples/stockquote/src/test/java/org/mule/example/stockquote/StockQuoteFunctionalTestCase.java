/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.stockquote;

import org.mule.tck.FunctionalTestCase;
import org.mule.tck.util.WebServiceOnlineCheck;
import org.mule.util.StringUtils;

import java.util.Locale;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;


public class StockQuoteFunctionalTestCase extends FunctionalTestCase
{
    public StockQuoteFunctionalTestCase()
    {
        // Do not fail test case upon timeout because this probably just means
        // that the 3rd-party web service is off-line.
        setFailOnTimeout(false);
    }
    
    /**
     * If a simple call to the web service indicates that it is not responding properly,
     * we disable the test case so as to not report a test failure which has nothing to do
     * with Mule.
     *
     * see EE-947
     */
    @Override
    protected boolean isDisabledInThisEnvironment()
    {
        return (WebServiceOnlineCheck.isWebServiceOnline() == false);
    }

    @Override
    protected String getConfigResources()
    {
        return "mule-config.xml";
    }

    public void testREST() throws Exception
    {
        runTest("REST");
    }
    
    public void testSOAP() throws Exception
    {
        runTest("SOAP");
    }

    public void testWSDL() throws Exception
    {
        runTest("WSDL");
    }

    private void runTest(String method) throws Exception
    {
        String url = String.format("http://localhost:48309/stockquote?symbol=CSCO&method=%1s", method);
        GetMethod request = new GetMethod(url);
        new HttpClient().executeMethod(request);
        
        String text = request.getResponseBodyAsString();
        assertTrue("Stock quote should contain \"CISCO\": " + text, StringUtils.containsIgnoreCase(text, "CISCO"));

        // the stockquote message is localized ...
        if (Locale.getDefault().getISO3Language().equalsIgnoreCase("eng"))
        {
            assertTrue("Stock quote should start with \"StockQuote[\":" + text, text.startsWith("StockQuote["));
        }
    }
}
