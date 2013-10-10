/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.vm.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

public class PersistentUnaddressedVmQueueTestCase extends AbstractServiceAndFlowTestCase
{

    private static final int RECEIVE_TIMEOUT = 5000;

    public PersistentUnaddressedVmQueueTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "vm/persistent-unaddressed-vm-queue-test-service.xml"},
            {ConfigVariant.FLOW, "vm/persistent-unaddressed-vm-queue-test-flow.xml"}});
    }

    @Test
    public void testAsynchronousDispatching() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://receiver1?connector=Connector1", "Test", null);
        MuleMessage result = client.request("vm://out?connector=Connector2", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals(result.getPayloadAsString(), "Test");
    }
}
