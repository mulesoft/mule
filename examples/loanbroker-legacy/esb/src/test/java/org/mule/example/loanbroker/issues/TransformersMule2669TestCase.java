/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker.issues;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.example.loanbroker.bank.Bank;
import org.mule.example.loanbroker.messages.LoanBrokerQuoteRequest;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TransformersMule2669TestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "transformers-mule-2669.xml";
    }

    @Test
    public void testTransformers() throws MuleException
    {
        MuleClient client = new MuleClient(muleContext);
        LoanBrokerQuoteRequest request = new LoanBrokerQuoteRequest();
        request.setLenders(new Bank[0]);
        MuleMessage response = client.send("jms://in?connector=default", request, null);
        assertNotNull(response);
        logger.debug(response);
        assertNull(response.getExceptionPayload());
        assertTrue(response.getInboundPropertyNames().contains("recipients"));
    }

}
