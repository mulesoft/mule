/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.junit.Before;
import org.junit.Test;
import org.mule.DefaultMessageCollection;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.mule.TestMessageProcessor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ForeachTestCase extends AbstractMuleContextTestCase
{
    protected Foreach simpleForeach;
    protected Foreach nestedForeach;
    protected ArrayList<MuleEvent> processedEvents;

    private static String ERR_NUMBER_MESSAGES = "Not a correct number of messages processed";
    private static String ERR_PAYLOAD_TYPE = "Type error on processed payloads";
    private static String ERR_OUTPUT = "Messages processed incorrectly";

    @Before
    public void initialise() throws MuleException 
    {
        processedEvents = new ArrayList<MuleEvent>();
        simpleForeach = createForeach(getSimpleMessageProcessors());
        nestedForeach = createForeach(getNestedMessageProcessors());
    }

    private List<MessageProcessor> getSimpleMessageProcessors()
    {
        List<MessageProcessor> lmp = new ArrayList<MessageProcessor>();
        lmp.add(new MessageProcessor() {
            public MuleEvent process(MuleEvent event) {
                String payload = event.getMessage().getPayload().toString();
                event.getMessage().setPayload(payload + ":foo");
                processedEvents.add(event);
                return event;
            }
        });
        lmp.add(new TestMessageProcessor("zas"));
        return lmp;
    }

    private List<MessageProcessor> getNestedMessageProcessors() throws MuleException
    {
        List<MessageProcessor> lmp = new ArrayList<MessageProcessor>();
        Foreach internalForeach = new Foreach();
        internalForeach.setMessageProcessors(getSimpleMessageProcessors());
        lmp.add(internalForeach);
        return lmp;
    }

    private Foreach createForeach(List<MessageProcessor> mps)
            throws MuleException
    {
        Foreach f = new Foreach();
        f.setMessageProcessors(mps);
        f.setMuleContext(muleContext);
        f.initialise();
        return f;
    }

    @Test
    public void testArrayListPayload() throws Exception
    {
        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.add("bar");
        arrayList.add("zip");
        simpleForeach.process(getTestEvent(arrayList));

        assertSimpleProcessedMessages();
    }

    @Test
    public void testArrayPayload() throws Exception
    {        
        String[] array = new String[2];
        array[0] = "bar";
        array[1] = "zip";
        simpleForeach.process(getTestEvent(array));

        assertSimpleProcessedMessages();
    }

    @Test
    public void testMuleMessageCollectionPayload() throws Exception
    {
        MuleMessageCollection msgCollection = new DefaultMessageCollection(muleContext);
        MuleMessage msg = new DefaultMuleMessage("bar", muleContext);
        msgCollection.addMessage(msg);
        msg = new DefaultMuleMessage("zip", muleContext);
        msgCollection.addMessage(msg);
        simpleForeach.process(getTestEvent(msgCollection));

        assertSimpleProcessedMessages();
    }

    @Test
    public void testIterablePayload() throws Exception
    {
        Iterable<String> iterable = new DummySimpleIterableClass();
        simpleForeach.process(getTestEvent(iterable));

        assertSimpleProcessedMessages();
    }

    @Test
    public void testIteratorPayload() throws Exception
    {
        Iterable<String> iterable = new DummySimpleIterableClass();
        simpleForeach.process(getTestEvent(iterable.iterator()));

        assertSimpleProcessedMessages();
    }
    
    @Test
    public void testNestedArrayListPayload() throws Exception
    {
        ArrayList<ArrayList<String>> payload = new ArrayList<ArrayList<String>>();
        ArrayList<String> elem1 = new ArrayList<String>();
        ArrayList<String> elem2 = new ArrayList<String>();
        ArrayList<String> elem3 = new ArrayList<String>();
        elem1.add("a1");
        elem1.add("a2");
        elem1.add("a3");
        elem2.add("b1");
        elem2.add("b2");
        elem3.add("c1");
        payload.add(elem1);
        payload.add(elem2);
        payload.add(elem3);

        nestedForeach.process(getTestEvent(payload));
        assertNestedProcessedMessages();
    }

    @Test
    public void testNestedArrayPayload() throws Exception
    {
        String[][] payload = new String[3][2];
        payload[0][0] = "a1";
        payload[0][1] = "a2";
        payload[1][0] = "a3";
        payload[1][1] = "b1";
        payload[2][0] = "b2";
        payload[2][1] = "c1";

        nestedForeach.process(getTestEvent(payload));
        assertNestedProcessedMessages();
    }

    @Test
    public void testNestedMuleMessageCollectionPayload() throws Exception
    {
        MuleMessageCollection parentCollection = new DefaultMessageCollection(muleContext);
        MuleMessageCollection childCollection1 = new DefaultMessageCollection(muleContext);
        MuleMessageCollection childCollection2 = new DefaultMessageCollection(muleContext);
        MuleMessage msg;
        msg = new DefaultMuleMessage("a1", muleContext);
        childCollection1.addMessage(msg);
        msg = new DefaultMuleMessage("a2", muleContext);
        childCollection1.addMessage(msg);
        msg = new DefaultMuleMessage("a3", muleContext);
        childCollection1.addMessage(msg);

        msg = new DefaultMuleMessage("b1", muleContext);
        childCollection2.addMessage(msg);
        msg = new DefaultMuleMessage("b2", muleContext);
        childCollection2.addMessage(msg);
        msg = new DefaultMuleMessage("c1", muleContext);
        childCollection2.addMessage(msg);

        parentCollection.addMessage(childCollection1);
        parentCollection.addMessage(childCollection2);

        nestedForeach.process(getTestEvent(parentCollection));
        assertNestedProcessedMessages();
    }

    @Test
    public void testNestedIterablePayload() throws Exception
    {
        Iterable iterable = new DummyNestedIterableClass();

        nestedForeach.process(getTestEvent(iterable));
        assertNestedProcessedMessages();
    }

    @Test
    public void testNestedIteratorPayload() throws Exception
    {
        Iterable iterable = new DummyNestedIterableClass();

        nestedForeach.process(getTestEvent(iterable.iterator()));
        assertNestedProcessedMessages();
    }
    
    private void assertSimpleProcessedMessages()
    {
        assertEquals(ERR_NUMBER_MESSAGES, 2, processedEvents.size());
        assertTrue(ERR_PAYLOAD_TYPE, processedEvents.get(0).getMessage().getPayload() instanceof String);
        assertTrue(ERR_PAYLOAD_TYPE, processedEvents.get(1).getMessage().getPayload() instanceof String);
        assertEquals(ERR_OUTPUT, "bar:foo:zas", processedEvents.get(0).getMessage().getPayload());
        assertEquals(ERR_OUTPUT, "zip:foo:zas", processedEvents.get(1).getMessage().getPayload());
    }

    private void assertNestedProcessedMessages()
    {
        String[] expectedOutputs =
               {"a1:foo:zas",
                "a2:foo:zas",
                "a3:foo:zas",
                "b1:foo:zas",
                "b2:foo:zas",
                "c1:foo:zas" };
        assertEquals(ERR_NUMBER_MESSAGES, 6, processedEvents.size());
        for(int i = 0; i < processedEvents.size(); i++)
        {
            assertTrue(ERR_PAYLOAD_TYPE, processedEvents.get(i).getMessage().getPayload() instanceof String);    
        }
        for(int i = 0; i < processedEvents.size(); i++)
        {
            assertEquals(ERR_OUTPUT, expectedOutputs[i], processedEvents.get(i).getMessage().getPayload());
        }
    }

    public class DummySimpleIterableClass implements Iterable<String>
    {
        public ArrayList<String> strings = new ArrayList<String>();
        public DummySimpleIterableClass()
        {
            strings.add("bar");
            strings.add("zip");
        }

        @Override
        public Iterator<String> iterator() {
            return strings.iterator();
        }
    }

    private class DummyNestedIterableClass implements Iterable<DummySimpleIterableClass>
    {
        private ArrayList<DummySimpleIterableClass> iterables = new ArrayList<DummySimpleIterableClass>();
        public DummyNestedIterableClass()
        {
            DummySimpleIterableClass dsi1 = new DummySimpleIterableClass();
            dsi1.strings = new ArrayList<String>();
            dsi1.strings.add("a1");
            dsi1.strings.add("a2");
            DummySimpleIterableClass dsi2 = new DummySimpleIterableClass();
            dsi2.strings = new ArrayList<String>();
            dsi2.strings.add("a3");
            dsi2.strings.add("b1");
            dsi2.strings.add("b2");
            dsi2.strings.add("c1");
            iterables.add(dsi1);
            iterables.add(dsi2);
        }

        @Override
        public Iterator<DummySimpleIterableClass> iterator() {
            return iterables.iterator();
        }
    }

}
