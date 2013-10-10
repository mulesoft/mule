/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.vm.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.FileUtils;

import org.junit.BeforeClass;
import org.junit.Test;

public class PersistentUnhealthyMessageTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "vm/persistent-vmqueue-test.xml";
    }

    @BeforeClass
    public static void populateUnhealthyFiles() throws Exception
    {
        FileUtils.createFile(".mule/queuestore/flowOut/0-000-out-01.msg");
    }

    @Test
    public void testUnhealthyMessageIgnored() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://flowIn", "echo", null);
        MuleMessage result = client.request("vm://flowOut", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals("echo", result.getPayload());
    }
}


