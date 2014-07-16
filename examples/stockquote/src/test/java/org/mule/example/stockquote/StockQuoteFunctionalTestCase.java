/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.stockquote;

import static org.junit.Assert.assertTrue;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.util.WebServiceOnlineCheck;
import org.mule.transport.http.HttpConstants;
import org.mule.util.StringUtils;

import java.util.Locale;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Rule;
import org.junit.Test;

public class StockQuoteFunctionalTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected boolean isFailOnTimeout()
    {
        // Do not fail test case upon timeout because this probably just means
        // that the 3rd-party web service is off-line.
        return false;
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
    protected String getConfigFile()
    {
        return "mule-config.xml";
    }

    @Test
    public void testREST() throws Exception
    {
        runTest("REST");
    }
    
    @Test
    public void testSOAP() throws Exception
    {
        runTest("SOAP");
    }

    @Test
    public void testWSDL() throws Exception
    {
        runTest("WSDL");
    }

    private void runTest(String method) throws Exception
    {
        String url = String.format("http://localhost:" + dynamicPort.getNumber() + "/stockquote?symbol=CSCO&method=%1s", method);
        GetMethod request = new GetMethod(url);
        int responseCode = new HttpClient().executeMethod(request);
        
        String text = request.getResponseBodyAsString();

        // FIXME : there is still a chance this test will fail when the webservice
        // goes down in between tests
        if (responseCode == HttpConstants.SC_OK)
        {
            assertTrue("Stock quote should contain \"CISCO\": " + text, StringUtils.containsIgnoreCase(text, "CISCO"));
            //  the stockquote message is localized ...
            if (Locale.getDefault().getISO3Language().equalsIgnoreCase("eng"))
            {
                assertTrue("Stock quote should start with \"StockQuote[\":" + text, text.startsWith("StockQuote["));
            }
        }
        else
        {
            // don't fail if you don't get the correct http status code; it means the webservice is down again
            logger.warn("web service appears to be down again, so not failing the test");
        }
    }

}
