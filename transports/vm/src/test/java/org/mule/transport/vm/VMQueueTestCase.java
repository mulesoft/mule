/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

public class VMQueueTestCase extends AbstractServiceAndFlowTestCase
{

    public static final long WAIT = 3000L;

    public VMQueueTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }
    
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "vm/vm-queue-test-service.xml"},
            {ConfigVariant.FLOW, "vm/vm-queue-test-flow.xml"}
        });
    }

    @Test
    public void testSingleMessage() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("queue", "Marco", null);
        MuleMessage response = client.request("queue", WAIT);
        assertNotNull("Response is null", response);
        assertEquals("Marco", response.getPayload());
    }

    @Test
    public void testMultipleMessages() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Set polos = new HashSet(Arrays.asList(new String[]{"Marco", "Niccolo", "Maffeo"}));
        Iterator people = polos.iterator();
        while (people.hasNext())
        {
            client.dispatch("queue", people.next(), null);
        }

        for (int i = 0; i < 3; ++i)
        {
            MuleMessage response = client.request("queue", WAIT);
            assertNotNull("Response is null", response);
            String person = (String) response.getPayload();
            assertTrue(person, polos.contains(person));
            polos.remove(person);
        }
    }

    @Test
    public void testPassThrough() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Set polos = new HashSet(Arrays.asList(new String[]{"Marco", "Niccolo", "Maffeo"}));
        Iterator people = polos.iterator();
        while (people.hasNext())
        {
            client.dispatch("vm://entry", people.next(), null);
        }

        for (int i = 0; i < 3; ++i)
        {
            MuleMessage response = client.request("queue", WAIT);
            assertNotNull("Response is null", response);
            String person = (String) response.getPayload();
            String name = new StringTokenizer(person).nextToken();
            assertTrue(name, polos.contains(name));
            polos.remove(name);
        }
    }

    @Test
    public void testNamedEndpoint() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Set polos = new HashSet(Arrays.asList(new String[]{"Marco", "Niccolo", "Maffeo"}));
        Iterator people = polos.iterator();
        while (people.hasNext())
        {
            client.dispatch("entry", people.next(), null);
        }

        for (int i = 0; i < 3; ++i)
        {
            MuleMessage response = client.request("queue", WAIT);
            assertNotNull("Response is null", response);
            String person = (String) response.getPayload();
            String name = new StringTokenizer(person).nextToken();
            assertTrue(name, polos.contains(name));
            polos.remove(name);
        }
    }

}
