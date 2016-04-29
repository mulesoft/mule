/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.vm.functional.transactions;

import static org.junit.Assert.assertEquals;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import org.junit.Test;

public class TransactionWithRecipientListTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "vm/transaction-with-recipient-list-config.xml";
    }

    @Test
    public void testRecipientListRouterUseDefinedTransaction() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://input", "test", null);
        assertEquals("test Received", getPayloadAsString(response));
    }
}
