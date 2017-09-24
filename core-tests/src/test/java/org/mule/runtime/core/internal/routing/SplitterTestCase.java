/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.event.CoreEvent.Builder;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.internal.routing.outbound.IteratorMessageSequence;
import org.mule.runtime.core.privileged.event.DefaultMuleSession;
import org.mule.runtime.core.privileged.event.MuleSession;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SplitterTestCase extends AbstractMuleContextTestCase {

  private static final List<String> TEST_LIST_MULTIPLE = Arrays.asList("abc", "def", "ghi");
  private static final List<String> TEST_LIST_SINGLE = Arrays.asList("abc");

  public SplitterTestCase() {
    setStartContext(true);
  }

  /**
   * Tests that a collection payload can be routed properly
   */
  @Test
  public void testRouterCollection() throws Exception {
    assertRouted(TEST_LIST_MULTIPLE, 3, true);
  }

  @Test
  public void testRouterSingletonCollection() throws Exception {
    assertRouted(TEST_LIST_SINGLE, 1, true);
  }

  @Test
  public void testRouterArray() throws Exception {
    assertRouted(new String[] {"abc", "def", "ghi"}, 3, true);
  }

  /**
   * Tests that an iterable payload can be routed properly
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testRouterIterable() throws Exception {
    Iterable<String> mock = mock(Iterable.class);
    when(mock.iterator()).thenReturn(TEST_LIST_MULTIPLE.iterator());
    assertRouted(mock, 3, false);
    verify(mock, times(1)).iterator();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testRouterIterableSingleItem() throws Exception {
    Iterable<String> mock = mock(Iterable.class);
    when(mock.iterator()).thenReturn(TEST_LIST_SINGLE.iterator());
    assertRouted(mock, 1, false);
    verify(mock, times(1)).iterator();
  }

  /**
   * Tests that an iterator payload can be routed properly
   */
  @Test
  public void testRouterIterator() throws Exception {
    assertRouted(TEST_LIST_MULTIPLE.iterator(), 3, false);
  }

  /**
   * Tests that an iterator payload can be routed properly
   */
  @Test
  public void testRouterIteratorSingleItem() throws Exception {
    assertRouted(TEST_LIST_SINGLE.iterator(), 1, false);
  }

  /**
   * Tests that a message sequence payload can be routed properly
   */
  @Test
  public void testRouterMesseageSequence() throws Exception {
    assertRouted(new IteratorMessageSequence(TEST_LIST_MULTIPLE.iterator()), 3, false);
  }

  /**
   * Tests that an empty sequence can be routed properly
   */
  @Test
  public void testEmptySequence() throws Exception {
    Object payload = Collections.emptySet();
    MuleSession session = new DefaultMuleSession();
    Message toSplit = Message.of(payload);
    Splitter splitter = new Splitter();
    splitter.setMuleContext(muleContext);
    splitter.initialise();
    CoreEvent event = this.<PrivilegedEvent.Builder>getEventBuilder().message(toSplit).session(session).build();
    assertSame(event, splitter.process(event));
  }

  @Test
  public void testSingleMesseageSequence() throws Exception {
    assertRouted(new IteratorMessageSequence(TEST_LIST_SINGLE.iterator()), 1, false);
  }

  private void assertRouted(Object payload, int count, boolean counted) throws Exception, MuleException {
    MuleSession session = new DefaultMuleSession();

    Map<String, Serializable> inboundProps = new HashMap<>();
    inboundProps.put("inbound1", "1");
    inboundProps.put("inbound2", 2);
    inboundProps.put("inbound3", session);

    Map<String, Serializable> outboundProps = new HashMap<>();
    outboundProps.put("outbound1", "3");
    outboundProps.put("outbound2", 4);
    outboundProps.put("outbound3", session);

    Map<String, Object> invocationProps = new HashMap<>();
    invocationProps.put("invoke1", "5");
    invocationProps.put("invoke2", 6);
    invocationProps.put("invoke3", session);

    Set<Integer> expectedSequences = new HashSet<>();
    for (int i = 1; i <= count; i++) {
      expectedSequences.add(i);
    }

    Message toSplit =
        InternalMessage.builder().value(payload).inboundProperties(inboundProps).outboundProperties(outboundProps).build();
    Splitter splitter = new Splitter();
    Grabber grabber = new Grabber();
    splitter.setMuleContext(muleContext);
    splitter.setListener(grabber);
    splitter.initialise();

    final Builder eventBuilder = this.<PrivilegedEvent.Builder>getEventBuilder().message(toSplit).session(session);
    for (Map.Entry<String, Object> entry : invocationProps.entrySet()) {
      eventBuilder.addVariable(entry.getKey(), entry.getValue());
    }
    CoreEvent event = eventBuilder.build();

    splitter.process(event);
    List<CoreEvent> splits = grabber.getEvents();
    assertEquals(count, splits.size());

    Set<Object> actualSequences = new HashSet<>();
    assertSplitParts(count, counted, inboundProps, outboundProps, invocationProps, splits, actualSequences);
    assertEquals(expectedSequences, actualSequences);
  }

  private void assertSplitParts(int count, boolean counted, Map<String, Serializable> inboundProps,
                                Map<String, Serializable> outboundProps, Map<String, Object> invocationProps,
                                List<CoreEvent> splits, Set<Object> actualSequences) {
    for (CoreEvent event : splits) {
      Message msg = event.getMessage();
      assertTrue(msg.getPayload().getValue() instanceof String);
      if (counted) {
        assertThat(event.getGroupCorrelation().get().getGroupSize().getAsInt(), is(count));
      } else {
        assertThat(event.getGroupCorrelation().get().getGroupSize().isPresent(), is(false));
      }
      actualSequences.add(event.getGroupCorrelation().get().getSequence());
      String str = (String) msg.getPayload().getValue();
      assertTrue(TEST_LIST_MULTIPLE.contains(str));
      for (String key : inboundProps.keySet()) {
        assertEquals(((InternalMessage) msg).getInboundProperty(key), inboundProps.get(key));
      }
      for (String key : outboundProps.keySet()) {
        assertEquals(((InternalMessage) msg).getOutboundProperty(key), outboundProps.get(key));
      }
      for (String key : invocationProps.keySet()) {
        assertEquals(event.getVariables().get(key).getValue(), invocationProps.get(key));
      }
    }
  }

  private static class Grabber implements Processor {

    private List<CoreEvent> events = new ArrayList<>();

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      events.add(event);
      return null;
    }

    public List<CoreEvent> getEvents() {
      return events;
    }
  }
}
