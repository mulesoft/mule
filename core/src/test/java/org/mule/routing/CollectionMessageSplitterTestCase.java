/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.VoidMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.config.MuleProperties;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.service.Service;
import org.mule.routing.outbound.IteratorMessageSequence;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class CollectionMessageSplitterTestCase extends AbstractMuleContextTestCase
{
    private static final List<String> TEST_LIST_MULTIPLE = Arrays.asList("abc", "def", "ghi");
    private static final List<String> TEST_LIST_SINGLE = Arrays.asList("abc");

    public CollectionMessageSplitterTestCase()
    {
        setStartContext(true);
    }

    /**
     * Tests that a collection payload can be routed properly
     */
    @Test
    public void testRouterCollection() throws Exception
    {
        assertRouted(TEST_LIST_MULTIPLE, 3, true);
    }

    @Test
    public void testRouterSingletonCollection() throws Exception
    {
        assertRouted(TEST_LIST_SINGLE, 1, true);
    }

    @Test
    public void testRouterArray() throws Exception
    {
        assertRouted(new String[]{"abc", "def", "ghi"}, 3, true);
    }

    /**
     * Tests that an iterable payload can be routed properly
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testRouterIterable() throws Exception
    {
        Iterable<String> mock = mock(Iterable.class);
        when(mock.iterator()).thenReturn(TEST_LIST_MULTIPLE.iterator());
        assertRouted(mock, 3, false);
        verify(mock, times(1)).iterator();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRouterIterableSingleItem() throws Exception
    {
        Iterable<String> mock = mock(Iterable.class);
        when(mock.iterator()).thenReturn(TEST_LIST_SINGLE.iterator());
        assertRouted(mock, 1, false);
        verify(mock, times(1)).iterator();
    }

    /**
     * Tests that an iterator payload can be routed properly
     */
    @Test
    public void testRouterIterator() throws Exception
    {
        assertRouted(TEST_LIST_MULTIPLE.iterator(), 3, false);
    }

    /**
     * Tests that an iterator payload can be routed properly
     */
    @Test
    public void testRouterIteratorSingleItem() throws Exception
    {
        assertRouted(TEST_LIST_SINGLE.iterator(), 1, false);
    }

    /**
     * Tests that a message sequence payload can be routed properly
     */
    @Test
    public void testRouterMesseageSequence() throws Exception
    {
        assertRouted(new IteratorMessageSequence<String>(TEST_LIST_MULTIPLE.iterator()), 3, false);
    }

    /**
     * Tests that an empty sequence can be routed properly
     */
    @Test
    public void testEmptySequence() throws Exception
    {
        Object payload = Collections.emptySet();
        Service fc = getTestService();
        MuleSession session = getTestSession(fc, muleContext);
        MuleMessage toSplit = new DefaultMuleMessage(payload, new HashMap<String, Object>(),
            new HashMap<String, Object>(), null, muleContext);
        CollectionSplitter splitter = new CollectionSplitter();
        splitter.setMuleContext(muleContext);
        DefaultMuleEvent event = new DefaultMuleEvent(toSplit, getTestInboundEndpoint("ep"), fc, session);
        assertSame(VoidMuleEvent.getInstance(), splitter.process(event));
    }

    @Test
    public void testSingleMesseageSequence() throws Exception
    {
        assertRouted(new IteratorMessageSequence<String>(TEST_LIST_SINGLE.iterator()), 1, false);
    }

    private void assertRouted(Object payload, int count, boolean counted) throws Exception, MuleException
    {
        Service fc = getTestService();
        MuleSession session = getTestSession(fc, muleContext);

        Map<String, Object> inboundProps = new HashMap<String, Object>();
        inboundProps.put("inbound1", "1");
        inboundProps.put("inbound2", 2);
        inboundProps.put("inbound3", session);

        Map<String, Object> outboundProps = new HashMap<String, Object>();
        outboundProps.put("outbound1", "3");
        outboundProps.put("outbound2", 4);
        outboundProps.put("outbound3", session);

        Map<String, Object> invocationProps = new HashMap<String, Object>();
        invocationProps.put("invoke1", "5");
        invocationProps.put("invoke2", 6);
        invocationProps.put("invoke3", session);

        Set<Integer> expectedSequences = new HashSet<Integer>();
        for (int i = 1; i <= count; i++)
        {
            expectedSequences.add(i);
        }

        MuleMessage toSplit = new DefaultMuleMessage(payload, inboundProps, outboundProps, null, muleContext);
        for (Map.Entry<String, Object> entry : invocationProps.entrySet())
        {
            toSplit.setInvocationProperty(entry.getKey(), entry.getValue());
        }
        CollectionSplitter splitter = new CollectionSplitter();
        splitter.setMuleContext(muleContext);
        Grabber grabber = new Grabber();
        splitter.setListener(grabber);
        DefaultMuleEvent event = new DefaultMuleEvent(toSplit, getTestInboundEndpoint("ep"), fc, session);
        splitter.process(event);
        List<MuleMessage> splits = grabber.getMessages();
        assertEquals(count, splits.size());

        Set<Object> actualSequences = new HashSet<Object>();
        assertSplitParts(count, counted, inboundProps, outboundProps, invocationProps, splits,
            actualSequences);
        assertEquals(expectedSequences, actualSequences);
    }

    private void assertSplitParts(int count,
                                  boolean counted,
                                  Map<String, Object> inboundProps,
                                  Map<String, Object> outboundProps,
                                  Map<String, Object> invocationProps,
                                  List<MuleMessage> splits,
                                  Set<Object> actualSequences)
    {
        for (MuleMessage msg : splits)
        {
            assertTrue(msg.getPayload() instanceof String);
            assertEquals(counted ? count : -1,
                msg.getOutboundProperty(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY));
            actualSequences.add(msg.getOutboundProperty(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY));
            String str = (String)msg.getPayload();
            assertTrue(TEST_LIST_MULTIPLE.contains(str));
            for (String key : inboundProps.keySet())
            {
                assertEquals(msg.getInboundProperty(key), inboundProps.get(key));
            }
            for (String key : outboundProps.keySet())
            {
                assertEquals(msg.getOutboundProperty(key), outboundProps.get(key));
            }
            for (String key : invocationProps.keySet())
            {
                assertEquals(msg.getInvocationProperty(key), invocationProps.get(key));
            }
        }
    }

    private static class Grabber implements MessageProcessor
    {
        private List<MuleMessage> messages = new ArrayList<MuleMessage>();

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            messages.add(event.getMessage());
            return null;
        }

        public List<MuleMessage> getMessages()
        {
            return messages;
        }
    }
}
