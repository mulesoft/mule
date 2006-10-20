/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.routing.inbound;

import java.util.Iterator;

import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.routing.inbound.EventGroup;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.umo.UMOEvent;
import org.mule.util.UUID;

public class EventGroupTestCase extends AbstractMuleTestCase
{

    public void testConcurrentIteration() throws Exception
    {
        EventGroup eg = new EventGroup(UUID.getUUID());
        assertFalse(eg.iterator().hasNext());

        // add to events to start with
        eg.addEvent(MuleTestUtils.getTestEvent("foo1"));
        eg.addEvent(MuleTestUtils.getTestEvent("foo2"));
        assertTrue(eg.iterator().hasNext());

        // now add events while we iterate over the group
        Iterator i = eg.iterator();
        assertNotNull(i.next());
        eg.addEvent(MuleTestUtils.getTestEvent("foo3"));
        assertNotNull(i.next());
        eg.addEvent(MuleTestUtils.getTestEvent("foo4"));
        assertFalse(i.hasNext());

        // the added events should be in there though
        assertEquals(4, eg.size());
    }

    public void testToString() throws Exception
    {
        EventGroup eg = new EventGroup(UUID.getUUID());
        String es = eg.toString();
        assertTrue(es.endsWith("events=0"));

        UMOEvent e = MuleTestUtils.getTestEvent("foo");
        eg.addEvent(e);
        es = eg.toString();
        assertTrue(es.indexOf("events=1") != -1);
        assertTrue(es.endsWith("[" + e.getMessage().getUniqueId() + "]"));

        UMOEvent e2 = new MuleEvent(new MuleMessage("foo2"), e);
        eg.addEvent(e2);
        es = eg.toString();
        assertTrue(es.indexOf("events=2") != -1);
        assertTrue(es.endsWith(e.getMessage().getUniqueId() + ", " + e2.getMessage().getUniqueId() + "]"));
    }

}
