/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
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
    
        if(response != null) 
        { 
            if (response.getExceptionPayload() == null) 
            {            
                assertTrue("Stock quote should contain \"BLOCK\": " + response.getPayload(), 
                            StringUtils.contains(response.getPayloadAsString(), "BLOCK"));
            }
            else
            {
                fail("Exception occurred: " + response.getExceptionPayload());
            }
        }
        else
        {
            fail("No response message.");
        }
     }
}
