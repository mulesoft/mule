/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.stockquote;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Locale;

public abstract class AbstractStockQuoteFunctionalTestCase extends FunctionalTestCase
{
    public static final String TEST_URL = "http://www.webservicex.net/stockquote.asmx/GetQuote?symbol=CSCO";

    public AbstractStockQuoteFunctionalTestCase()
    {
        super();
        
        // Do not fail test case upon timeout because this probably just means
        // that the 3rd-party web service is off-line.
        setFailOnTimeout(false);
    }
    
    /**
     * If a simple call to the web service indicates that it is not responding properly,
     * we disable the test case so as to not report a test failure which has nothing to do
     * with Mule.
     * 
     * @see EE-947
     */
    protected boolean isDisabledInThisEnvironment()
    {
        return !isWebServiceOnline();
    }
    
    /**
     * @return true if the web service is functioning correctly
     */
    protected boolean isWebServiceOnline()
    {
        logger.debug("Verifying that the web service is on-line...");
        
        BufferedReader input = null;
        try 
        {
            input = new BufferedReader(new InputStreamReader(new URL(TEST_URL).openStream()));

            String response = "";
            String line;
            while ((line = input.readLine()) != null) 
            {
                response += line;
            }

            if (StringUtils.containsIgnoreCase(response, "Cisco"))
            {
                return true;
            }
            else
            {
                logger.warn("Unexpected response, web service does not seem to be on-line: \n" + response);
                return false;
            }
        } 
        catch (Exception e) 
        {
            logger.warn("Exception occurred, web service does not seem to be on-line: " + e);
            return false;
        } 
        finally
        {
            if (input != null)
            {
                try
                {
                    input.close();
                }
                catch (IOException ioe) {}
            }
        }
    }

    public void testStockQuoteExample() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage response = client.send("vm://stockquote", "CSCO", null);

        if (null == response)
        {
            fail("No response message.");
        }
        else
        {
            if (null == response.getExceptionPayload())
            {
                String text = response.getPayloadAsString();
                assertNotNull("Null response", text);
                assertTrue("Stock quote should contain \"CISCO\": " + text, StringUtils.containsIgnoreCase(text, "CISCO"));
                if (Locale.getDefault().getISO3Language().equalsIgnoreCase("eng"))
                {
                    // the stockquote message is localized ...
                    assertTrue("Stock quote should start with \"StockQuote[\":" + text, text.startsWith("StockQuote["));
                }
                logger.debug("**********");
                logger.debug(response.getPayload());
                logger.debug(response.getPayloadAsString());
                logger.debug("**********");
            }
            else
            {
                fail("Exception occurred: " + response.getExceptionPayload());
            }
        }
    }
}
