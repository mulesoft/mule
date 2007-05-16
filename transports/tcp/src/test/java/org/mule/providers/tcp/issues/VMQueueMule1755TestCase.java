/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.issues;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 * This test is here to reduce circular dependencies
 * (tcp depends on vm, don't want to add the other way round).
 *
 * This test can be deleted once VMQueueTestCase in VM works OK
 * (this version uses TCP to avoid two kinds fof VM connector and
 * works fine - VMQueueTestCase currently fails).
 */
public class VMQueueMule1755TestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "vm-queue-test.xml";
    }

    public void testSingleMessage() throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("queue", "Marco", null);
        UMOMessage response = client.receive("queue", 1000);
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
            UMOMessage response = client.receive("queue", 1000);
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
            client.dispatch("tcp://localhost:65432", people.next(), null);
        }

        for (int i = 0; i < 3; ++i)
        {
            UMOMessage response = client.receive("queue", 1000);
            assertNotNull("Response is null", response);
            String person = (String) response.getPayload();
            StringTokenizer names = new StringTokenizer(person);
            assertTrue(person, polos.contains(names.nextToken()));
            polos.remove(person);
        }
    }

}