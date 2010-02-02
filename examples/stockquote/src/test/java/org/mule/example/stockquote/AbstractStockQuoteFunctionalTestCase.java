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
import org.mule.tck.util.WebServiceOnlineCheck;
import org.mule.util.StringUtils;

import java.util.Locale;

public abstract class AbstractStockQuoteFunctionalTestCase extends FunctionalTestCase
{
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
     * see EE-947
     */
    @Override
    protected boolean isDisabledInThisEnvironment()
    {
        return (WebServiceOnlineCheck.isWebServiceOnline() == false);
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
