/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.stockquote;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;
import org.mule.util.StringUtils;

public abstract class AbstractStockQuoteFunctionalTestCase extends FunctionalTestCase
{

    public void testStockQuoteExample() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage response = client.send("vm://stockquote", "HRB", null);
    
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
                assertTrue("Stock quote should contain \"BLOCK\": " + text, StringUtils.contains(text, "BLOCK"));
                assertTrue("Stock quote should start with \"StockQuote[\":" + text, text.startsWith("StockQuote["));
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
