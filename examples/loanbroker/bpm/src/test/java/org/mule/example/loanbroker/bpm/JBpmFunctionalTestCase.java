/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.loanbroker.bpm;

import org.mule.api.MuleMessage;
import org.mule.example.loanbroker.messages.Customer;
import org.mule.example.loanbroker.messages.CustomerQuoteRequest;
import org.mule.example.loanbroker.messages.LoanQuote;
import org.mule.example.loanbroker.tests.AbstractAsynchronousLoanBrokerTestCase;
import org.mule.module.client.MuleClient;
import org.mule.tck.util.MuleDerbyTestUtils;
import org.mule.transport.NullPayload;
import org.mule.transport.bpm.ProcessConnector;
import org.mule.transport.jbpm.Jbpm;


public class JBpmFunctionalTestCase extends AbstractAsynchronousLoanBrokerTestCase
{
    public JBpmFunctionalTestCase()
    {
        super();
        setDisposeManagerPerSuite(true);        
    }
    
    @Override
    protected String getConfigResources()
    {
        return "loan-broker-bpm-mule-config.xml";
    }

    @Override
    protected int getNumberOfRequests()
    {
        return 100;
    }

    @Override
    protected void suitePreSetUp() throws Exception
    {
        // set the derby.system.home system property to make sure that all derby databases are
        // created in maven's target directory       
        MuleDerbyTestUtils.defaultDerbyCleanAndInit("derby.properties", "database.name");

        super.suitePreSetUp();
    }

    @Override
    protected void doSetUp() throws Exception 
    {
        ProcessConnector jBpmConnector = (ProcessConnector) muleContext.getRegistry().lookupConnector("jBpmConnector");
        ((Jbpm) jBpmConnector.getBpms()).deployProcess("loan-broker-process.jpdl.xml");     
    }

    @Override
    public void testLotsOfLoanRequests() throws Exception
    {
        MuleClient client = new MuleClient();
        Customer c = new Customer("Ross Mason", 1234);
        CustomerQuoteRequest request = new CustomerQuoteRequest(c, 100000, 48);

        for (int i = 0; i < getNumberOfRequests(); i++)
        {
            client.dispatch("CustomerRequests", request, null);
        }
        
        MuleMessage result;
        for (int i = 0; i < getNumberOfRequests(); i++)
        {
            result = client.request("CustomerResponses", getDelay());
            assertNotNull("Result is null", result);
            assertFalse("Result is null", result.getPayload() instanceof NullPayload);
            assertTrue("Result should be LoanQuote but is " + result.getPayload().getClass().getName(),
                    result.getPayload() instanceof LoanQuote);
            LoanQuote quote = (LoanQuote) result.getPayload();
            assertTrue(quote.getInterestRate() > 0);
        }
    }
}
