/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.loanbroker;

import org.mule.api.MuleMessage;
import org.mule.example.loanbroker.message.CustomerQuoteRequest;
import org.mule.example.loanbroker.model.Customer;
import org.mule.example.loanbroker.model.LoanQuote;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transformer.simple.ByteArrayToObject;
import org.mule.transport.NullPayload;

public class LoanBrokerSyncTestCase extends FunctionalTestCase
{
    
    @Override
    protected String getConfigResources()
    {
        return "mule-config.xml";
    }

    public void testLoanBroker() throws Exception
    {
        muleContext.getRegistry().registerObject("streamToObjectTransformer", new ByteArrayToObject());
        MuleClient client = new MuleClient(muleContext);
        Customer c = new Customer("Ross Mason", 1234);
        CustomerQuoteRequest request = new CustomerQuoteRequest(c, 100000, 48);
        MuleMessage result = client.send("http://localhost:8080?responseTransformers=streamToObjectTransformer", request, null);
        assertNotNull("Result is null", result);
        assertFalse("Result is null", result.getPayload() instanceof NullPayload);
        assertTrue("Result should be LoanQuote but is " + result.getPayload().getClass().getName(), 
                    result.getPayload(Object.class) instanceof LoanQuote);
        LoanQuote quote = (LoanQuote)result.getPayload();
        assertTrue(quote.getInterestRate() > 0);
    }


    
}


