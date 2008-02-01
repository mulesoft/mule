/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.vm.functional;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transaction.TransactionCoordination;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class VmTransactionTestCase extends FunctionalTestCase
{
    protected static volatile boolean success = true;
    protected static final Log logger = LogFactory.getLog(VmTransactionTestCase.class);

    protected String getConfigResources()
    {
        return "vm/vm-transaction.xml";
    }

    public void testTransactionQueueEventsTrue() throws Exception
    {
        success = true;
        MuleClient client = new MuleClient();
        client.dispatch("vm://in?connector=vm", "TEST", null);
        MuleMessage message = client.request("vm://out?connector=vm", 10000);
        assertNotNull(message);
        assertTrue(success);

    }

    public void testTransactionSyncEndpoint() throws Exception
    {
        success = true;
        MuleClient client = new MuleClient();
        MuleMessage message = client.send("vm://sync?connector=vm", "TEST", null);
        assertNotNull(message);
        assertTrue(success);

    }

    public void testTransactionQueueEventsFalse() throws Exception
    {
        success = true;
        MuleClient client = new MuleClient();
        client.dispatch("vm://int3?connector=vmOnFly", "TEST", null);
        MuleMessage message = client.request("vm://outt3?connector=vm", 10000);
        assertNotNull(message);
        assertTrue(success);

    }


    public static class TestComponent
    {

        public Object process(Object a) throws Exception
        {
            if (TransactionCoordination.getInstance().getTransaction() == null)
            {
                success = false;
                logger.error("Transction is null");
            }
            return a;
        }

    }

}
