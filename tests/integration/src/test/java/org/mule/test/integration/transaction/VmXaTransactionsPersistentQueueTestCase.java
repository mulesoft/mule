/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.transaction;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class VmXaTransactionsPersistentQueueTestCase extends FunctionalTestCase
{

    private static final String TEST_MESSAGE = "TEST_MESSAGE";

    private final long timeout = getTestTimeoutSecs() * 1000 / 30;

    protected String getConfigResources()
    {
        return "org/mule/test/integration/transaction/vm-xa-transaction-persistent-queue.xml";
    }

    public void testOutboundRouterTransactions() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage msg = client.send("vm://in", TEST_MESSAGE, null, (int) timeout);

        assertNotNull(msg);
        assertNull(msg.getExceptionPayload());
        assertEquals("Wrong message returned", TEST_MESSAGE + " Received", msg.getPayload());
    }

}