/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.vm.functional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.transaction.TransactionCoordination;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

public class VmTransactionTestCase extends FunctionalTestCase
{

    protected static volatile boolean serviceComponentAck = false;
    protected static final Log logger = LogFactory.getLog(VmTransactionTestCase.class);

    @Override
    protected String getConfigFile()
    {
        return "vm/vm-transaction-flow.xml";
    }

    @Test
    public void testDispatch() throws Exception
    {
        serviceComponentAck = false;
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://dispatchIn", "TEST", null);
        MuleMessage message = client.request("vm://out", 10000);
        assertNotNull("Message", message);
    }

    @Test
    public void testSend() throws Exception
    {
        serviceComponentAck = false;
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://sendRequestIn", "TEST", null);
        assertNotNull("Message", message);
        assertTrue("Service component acknowledgement", serviceComponentAck);
    }

    public static class TestComponent
    {
        public Object process(Object message) throws Exception
        {
            if (TransactionCoordination.getInstance().getTransaction() != null)
            {
                serviceComponentAck = true;
            }
            return message;
        }

    }

}
