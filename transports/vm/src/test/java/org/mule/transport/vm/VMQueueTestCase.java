/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.vm;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class VMQueueTestCase extends FunctionalTestCase
{

    public static final long WAIT = 3000L;

    @Override
    protected String getConfigResources()
    {
        return "vm/vm-queue-test.xml";
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
