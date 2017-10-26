/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.mule.DefaultMessageCollection;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.registry.RegistrationException;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.PartitionableObjectStore;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.UUID;
import org.mule.util.store.DefaultObjectStoreFactoryBean;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.collections.IteratorUtils;
import org.junit.Before;
import org.junit.Test;

public class EventGroupTestCase extends AbstractMuleContextTestCase
{

    private PartitionableObjectStore<WrapperOrderEvent> objectStore;

    @Before
    public void before() throws RegistrationException
    {
        objectStore = (PartitionableObjectStore) muleContext.getRegistry().lookupObject(DefaultObjectStoreFactoryBean.class).createDefaultInMemoryObjectStore();
    }

    @Test
    public void concurrentIteration() throws Exception
    {
        EventGroup eg = new EventGroup(UUID.getUUID(),muleContext);
        eg.initEventsStore(objectStore);
        assertFalse(eg.iterator().hasNext());

        // add to events to start with
        eg.addEvent(getTestEvent("foo1"));
        eg.addEvent(getTestEvent("foo2"));
        assertTrue(eg.iterator().hasNext());

        // now add events while we iterate over the group
        Iterator<MuleEvent> i = eg.iterator();
        assertNotNull(i.next());
        eg.addEvent(getTestEvent("foo3"));
        assertNotNull(i.next());
        eg.addEvent(getTestEvent("foo4"));
        assertFalse(i.hasNext());

        // the added events should be in there though
        assertEquals(4, eg.size());
    }

    @Test
    public void eventGroupEquality() throws ObjectStoreException
    {
        EventGroup g1 = new EventGroup("foo",muleContext);
        g1.initEventsStore(objectStore);
        EventGroup g2 = new EventGroup("foo",muleContext);
        g2.initEventsStore(objectStore);
        EventGroup g3 = new EventGroup("bar",muleContext);
        g3.initEventsStore(objectStore);

        assertEquals(g1, g2);
        assertFalse(g1.equals(g3));

        MyEventGroup mg = new MyEventGroup("foo");
        assertEquals(g1, mg);
        assertEquals(mg, g1);

        mg = new MyEventGroup("bar");
        assertFalse(g1.equals(mg));
        assertFalse(mg.equals(g1));
    }

    @Test
    public void eventGroupHashCode() throws ObjectStoreException
    {
        String uuid = UUID.getUUID();
        EventGroup g1 = new EventGroup(uuid,muleContext);
        g1.initEventsStore(objectStore);
        EventGroup g2 = new EventGroup(uuid,muleContext);
        g2.initEventsStore(objectStore);
        EventGroup g3 = new EventGroup(UUID.getUUID(),muleContext);
        g3.initEventsStore(objectStore);

        assertEquals(g1.hashCode(), g2.hashCode());
        assertEquals(g1, g2);

        assertFalse(g1.hashCode() == g3.hashCode());
        assertFalse(g1.equals(g3));
        assertFalse(g3.equals(g1));

        // now test Set compatibility
        Set<EventGroup> s = new HashSet<EventGroup>();
        s.add(g1);

        // make sure g1 is in the set
        assertTrue(s.contains(g1));
        assertEquals(1, s.size());

        // g2 has the same hash, so it should match
        assertTrue(s.contains(g2));
        // even though there is only one object in the set
        assertEquals(1, s.size());

        // make sure g3 cannot be found
        assertFalse(s.contains(g3));
        // now add it
        assertTrue(s.add(g3));
        // make sure it is in there
        assertTrue(s.contains(g3));
        // make sure it is really in there
        assertEquals(2, s.size());
    }

    @Test
    public void eventGroupComparison() throws InterruptedException, ObjectStoreException
    {
        String uuid = UUID.getUUID();
        EventGroup g1 = new EventGroup(uuid,muleContext);
        g1.initEventsStore(objectStore);
        EventGroup g2 = new EventGroup(uuid,muleContext);
        g2.initEventsStore(objectStore);
        EventGroup g3 = new EventGroup(UUID.getUUID(),muleContext);
        g3.initEventsStore(objectStore);

        // test comparison against null
        try
        {
            g1.compareTo(null);
            fail("expected NullPointerException");
        }
        catch (NullPointerException npe)
        {
            // expected
        }

        assertEquals(0, g1.compareTo(g2));
        /*
         * guids are randomly generated, we cannot compare them with '<' '>'
         * we used to generate them this way: generator.generateTimeBasedUUID().toString()
         * but now we generate them as java.util.UUID.randomUUID().toString()
         */
        assertTrue(g1.compareTo(g3) != 0);
        assertTrue(g3.compareTo(g1) != 0);
        assertTrue(g3.compareTo(g2) != 0);

        // when the groupId is not Comparable, the creation time is used as fallback
        g1 = new EventGroup(new Object(),muleContext);
        // sleep a mini bit to ensure that both event groups do not accidentially have the same
        // creation timestamp
        Thread.sleep(10);
        g2 = new EventGroup(new Object(),muleContext);

        // g1 is older (smaller) than g2
        assertTrue(g1.compareTo(g2) < 0);
        assertTrue(g2.compareTo(g1) > 0);
    }

    @Test
    public void eventGroupConversionToArray() throws Exception
    {
        EventGroup eg = new EventGroup(UUID.getUUID(),muleContext);
        eg.initEventsStore(objectStore);
        eg.addEvent(getTestEvent("foo1"));
        eg.addEvent(getTestEvent("foo2"));

        Object[] array1 = IteratorUtils.toArray(eg.iterator(false));
        MuleEvent[] array2 = eg.toArray(false);
        assertTrue(Arrays.equals(array1, array2));
    }

    @Test
    public void eventGroupConversionToString() throws Exception
    {
        EventGroup eg = new EventGroup(UUID.getUUID(), muleContext);
        eg.initEventsStore(objectStore);
        String es = eg.toString();
        assertTrue(es.endsWith("events=0}"));

        MuleEvent firstEvent = getTestEvent("foo");
        String firstId = firstEvent.getMessage().getUniqueId();
        eg.addEvent(firstEvent);
        es = eg.toString();
        assertTrue(es.contains("events=1"));
        assertTrue(es.endsWith("[" + firstId + "]}"));

        MuleEvent secondEvent = new DefaultMuleEvent(new DefaultMuleMessage("foo2", muleContext),
            getTestEvent("foo2"));
        String secondId = secondEvent.getMessage().getUniqueId();
        eg.addEvent(secondEvent);
        es = eg.toString();
        assertTrue(es.contains("events=2"));
        assertTrue(es.contains(firstId));
        assertTrue(es.contains(secondId));
    }

    @Test
    public void mergedSessions() throws Exception
    {
        EventGroup eg = new EventGroup(UUID.getUUID(),muleContext);
        eg.initEventsStore(objectStore);
        assertFalse(eg.iterator().hasNext());

        MuleEvent event1 = getTestEvent("foo1");
        MuleEvent event2 = getTestEvent("foo2");
        MuleEvent event3 = getTestEvent("foo3");

        event1.getSession().setProperty("key1", "value1");
        event1.getSession().setProperty("key2", "value2");
        event2.getSession().setProperty("KEY2", "value2NEW");
        event2.getSession().setProperty("key3", "value3");
        event3.getSession().setProperty("key4", "value4");

        eg.addEvent(event1);
        System.out.println(event1.getSession());
        eg.addEvent(event2);
        System.out.println(event2.getSession());
        eg.addEvent(event3);
        System.out.println(event3.getSession());

        MuleEvent result = eg.getMessageCollectionEvent();
        assertEquals("value1", result.getSession().getProperty("key1"));
        // Cannot assert this because the ordering of events aren't ordered. See MULE-5998
        //assertEquals("value2NEW", result.getSession().getProperty("key2"));
        assertEquals("value3", result.getSession().getProperty("key3"));
        assertEquals("value4", result.getSession().getProperty("key4"));
    }


    @Test
    public void arrivalOrderEvents() throws Exception
    {
        EventGroup eventGroup = new EventGroup(UUID.getUUID(),muleContext);
        eventGroup.initEventsStore(objectStore);
        addMuleEventsWithSharedFlowVarsToEventGroup(eventGroup);
        MuleEvent result = eventGroup.getMessageCollectionEvent();
        DefaultMessageCollection messageCollection = (DefaultMessageCollection) result.getMessage();
        MuleMessage messages [] =  messageCollection.getMessagesAsArray();

        assertThat(messages.length, is(3));
        assertThat(messages[0].getPayloadAsString(), is("foo0"));
        assertThat(messages[1].getPayloadAsString(), is("foo1"));
        assertThat(messages[2].getPayloadAsString(), is("foo2"));
    }

    private void addMuleEventsWithSharedFlowVarsToEventGroup(EventGroup group) throws Exception
    {
        MuleEvent testEvent = getTestEvent(null);

        for (int i = 0; i < 3; i++)
        {
            MuleMessage messageCopy = new DefaultMuleMessage(testEvent.getMessage());
            messageCopy.setPayload("foo" + i);
            messageCopy.setCorrelationSequence(i);
            group.addEvent(new DefaultMuleEvent(messageCopy, testEvent, true, true));
        }
    }

    @Test
    public void isDisposedEventGroupPartition() throws Exception
    {
        EventGroup eventGroup = new EventGroup(UUID.getUUID(), muleContext);
        PartitionableObjectStore objectStore = mock(PartitionableObjectStore.class);
        eventGroup.initEventsStore(objectStore);

        eventGroup.clear();

        verify(objectStore).disposePartition(anyString());
    }

    private static class MyEventGroup extends EventGroup
    {
        private static final long serialVersionUID = 1L;

        public MyEventGroup(Object groupId)
        {
            super(groupId,muleContext);
        }

        public MyEventGroup(Object groupId, int expectedSize)
        {
            super(groupId, muleContext, expectedSize, "EventGroupTestCase");
        }
    }
}
