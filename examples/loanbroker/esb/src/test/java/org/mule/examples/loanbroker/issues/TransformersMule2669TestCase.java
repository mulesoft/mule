/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker.issues;

import org.mule.examples.loanbroker.bank.Bank;
import org.mule.examples.loanbroker.messages.LoanBrokerQuoteRequest;
import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

import java.util.Iterator;
import java.util.Set;

public class TransformersMule2669TestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "transformers-mule-2669.xml";
    }

    public void testTransformers() throws UMOException
    {
        MuleClient client = new MuleClient();
        LoanBrokerQuoteRequest request = new LoanBrokerQuoteRequest();
        request.setLenders(new Bank[0]);
        UMOMessage response = client.send("jms://in?connector=default", request, null);
        assertNotNull(response);
        assertNull(response.getExceptionPayload());
        Set propertyNames = response.getPropertyNames();
        assertTrue(propertyNames.size() > 0);
        Iterator names = propertyNames.iterator();
        while (names.hasNext())
        {
            logger.debug(names.next());
        }
        assertTrue(propertyNames.contains("recipients"));
    }

}
