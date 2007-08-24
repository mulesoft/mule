/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.vm;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

public class VMQueueTestCase extends FunctionalTestCase
{

    public static final long WAIT = 3000L;

    protected String getConfigResources()
    {
        return "vm/vm-queue-test.xml";
    }

    public void testSingleMessage() throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("queue", "Marco", null);
        UMOMessage response = client.receive("queue", WAIT);
        assertNotNull("Response is null", response);
        assertEquals("Marco", response.getPayload());
    }

    public void testMultipleMessages() throws Exception
    {
        MuleClient client = new MuleClient();
        Set polos = new HashSet(Arrays.asList(new String[]{"Marco", "Niccolo", "Maffeo"}));
        Iterator people = polos.iterator();
        while (people.hasNext())
        {
            client.dispatch("queue", people.next(), null);
        }

        for (int i = 0; i < 3; ++i)
        {
            UMOMessage response = client.receive("queue", WAIT);
            assertNotNull("Response is null", response);
            String person = (String) response.getPayload();
            assertTrue(person, polos.contains(person));
            polos.remove(person);
        }
    }

    public void testPassThrough() throws Exception
    {
        MuleClient client = new MuleClient();
        Set polos = new HashSet(Arrays.asList(new String[]{"Marco", "Niccolo", "Maffeo"}));
        Iterator people = polos.iterator();
        while (people.hasNext())
        {
            client.dispatch("vm://entry?connector=vm-normal", people.next(), null);
        }

        for (int i = 0; i < 3; ++i)
        {
            UMOMessage response = client.receive("queue", WAIT);
            assertNotNull("Response is null", response);
            String person = (String) response.getPayload();
            String name = new StringTokenizer(person).nextToken();
            assertTrue(name, polos.contains(name));
            polos.remove(name);
        }
    }

    public void testNamedEndpoint() throws Exception
    {
        MuleClient client = new MuleClient();
        Set polos = new HashSet(Arrays.asList(new String[]{"Marco", "Niccolo", "Maffeo"}));
        Iterator people = polos.iterator();
        while (people.hasNext())
        {
            client.dispatch("entry", people.next(), null);
        }

        for (int i = 0; i < 3; ++i)
        {
            UMOMessage response = client.receive("queue", WAIT);
            assertNotNull("Response is null", response);
            String person = (String) response.getPayload();
            String name = new StringTokenizer(person).nextToken();
            assertTrue(name, polos.contains(name));
            polos.remove(name);
        }
    }

}