/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.loanbroker.issues;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.example.loanbroker.bank.Bank;
import org.mule.example.loanbroker.messages.LoanBrokerQuoteRequest;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class TransformersMule2669TestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "transformers-mule-2669.xml";
    }

    public void testTransformers() throws MuleException
    {
        MuleClient client = new MuleClient(muleContext);
        LoanBrokerQuoteRequest request = new LoanBrokerQuoteRequest();
        request.setLenders(new Bank[0]);
        MuleMessage response = client.send("jms://in?connector=default", request, null);
        assertNotNull(response);
        logger.debug(response);
        assertNull(response.getExceptionPayload());
        assertTrue(response.getPropertyNames(PropertyScope.INBOUND).contains("recipients"));
    }

}
