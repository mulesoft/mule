/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.vm.functional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.transaction.TransactionCoordination;

public class VmTransactionTestCase extends AbstractServiceAndFlowTestCase
{    
    protected static volatile boolean serviceComponentAck = false;
    protected static final Log logger = LogFactory.getLog(VmTransactionTestCase.class);

    public VmTransactionTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }
    
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "vm/vm-transaction-service.xml"},
            {ConfigVariant.FLOW, "vm/vm-transaction-flow.xml"}
        });
    }
   
    @Test
    public void testDispatch() throws Exception
    {
        serviceComponentAck = false;
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://dispatchIn", "TEST", null);
        MuleMessage message = client.request("vm://out", 10000);
        assertNotNull("Message", message);
    }

    @Test
    public void testSend() throws Exception
    {
        serviceComponentAck = false;
        MuleClient client = new MuleClient(muleContext);
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
