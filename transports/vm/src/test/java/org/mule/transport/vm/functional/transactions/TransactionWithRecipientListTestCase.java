/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.vm.functional.transactions;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TransactionWithRecipientListTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "vm/transaction-with-recipient-list-config.xml";
    }

    @Test
    public void testRecipientListRouterUseDefinedTransaction() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://input", "test", (Map) null);

        assertEquals("test Received", response.getPayloadAsString());
    }
}
