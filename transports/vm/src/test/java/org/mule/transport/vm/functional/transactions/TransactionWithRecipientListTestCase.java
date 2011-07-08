/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.vm.functional.transactions;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import java.util.Map;

public class TransactionWithRecipientListTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "vm/transaction-with-recipient-list-config.xml";
    }

    public void testRecipientListRouterUseDefinedTransaction() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://input", "test", (Map) null);

        assertEquals("test Received", response.getPayloadAsString());
    }
}
