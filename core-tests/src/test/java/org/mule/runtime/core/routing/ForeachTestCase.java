/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.construct.Flow;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.mule.TestMessageProcessor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class ForeachTestCase extends AbstractMuleContextTestCase {

  protected Foreach simpleForeach;
  protected Foreach nestedForeach;
  protected ArrayList<Event> processedEvents;

  private static String ERR_NUMBER_MESSAGES = "Not a correct number of messages processed";
  private static String ERR_PAYLOAD_TYPE = "Type error on processed payloads";
  private static String ERR_OUTPUT = "Messages processed incorrectly";

  @Before
  public void initialise() throws MuleException {
    processedEvents = new ArrayList<>();
    simpleForeach = createForeach(getSimpleMessageProcessors());
    nestedForeach = createForeach(getNestedMessageProcessors());
  }

  private List<Processor> getSimpleMessageProcessors() {
    List<Processor> lmp = new ArrayList<>();
    lmp.add(event -> {
      String payload = event.getMessage().getPayload().getValue().toString();
      event = Event.builder(event).message(InternalMessage.builder(event.getMessage()).payload(payload + ":foo").build()).build();
      return event;
    });
    lmp.add(new TestMessageProcessor("zas"));
    lmp.add(event -> {
      processedEvents.add(event);
      return event;
    });
    return lmp;
  }

  private List<Processor> getNestedMessageProcessors() throws MuleException {
    List<Processor> lmp = new ArrayList<>();
    Foreach internalForeach = new Foreach();
    internalForeach.setMessageProcessors(getSimpleMessageProcessors());
    lmp.add(internalForeach);
    return lmp;
  }

  private Foreach createForeach(List<Processor> mps) throws MuleException {
    Foreach foreachMp = new Foreach();
    foreachMp.setMessageProcessors(mps);
    foreachMp.setMuleContext(muleContext);
    foreachMp.initialise();
    return foreachMp;
  }

  @Test
  public void arrayListPayload() throws Exception {
    List<String> arrayList = new ArrayList<>();
    arrayList.add("bar");
    arrayList.add("zip");
    simpleForeach.process(eventBuilder().message(InternalMessage.of(arrayList)).build());

    assertSimpleProcessedMessages();
  }

  @Test
  public void arrayPayload() throws Exception {
    String[] array = new String[2];
    array[0] = "bar";
    array[1] = "zip";
    simpleForeach.process(eventBuilder().message(InternalMessage.of(array)).build());

    assertSimpleProcessedMessages();
  }

  @Test
  public void muleMessageCollectionPayload() throws Exception {
    List<InternalMessage> list = new ArrayList<>();
    list.add(InternalMessage.builder().payload("bar").build());
    list.add(InternalMessage.builder().payload("zip").build());
    InternalMessage msgCollection = InternalMessage.builder().payload(list).build();
    simpleForeach.process(eventBuilder().message(msgCollection).build());

    assertSimpleProcessedMessages();
  }

  @Test
  public void iterablePayload() throws Exception {
    Iterable<String> iterable = new DummySimpleIterableClass();
    final Event testEvent = eventBuilder().message(InternalMessage.of(iterable)).build();
    simpleForeach.process(testEvent);

    assertSimpleProcessedMessages();
  }

  @Test
  public void iteratorPayload() throws Exception {
    Iterable<String> iterable = new DummySimpleIterableClass();
    simpleForeach.process(eventBuilder().message(InternalMessage.of(iterable.iterator())).build());

    assertSimpleProcessedMessages();
  }

  @Test
  public void nestedArrayListPayload() throws Exception {
    List<List<String>> payload = new ArrayList<>();
    List<String> elem1 = new ArrayList<>();
    List<String> elem2 = new ArrayList<>();
    List<String> elem3 = new ArrayList<>();
    elem1.add("a1");
    elem1.add("a2");
    elem1.add("a3");
    elem2.add("b1");
    elem2.add("b2");
    elem3.add("c1");
    payload.add(elem1);
    payload.add(elem2);
    payload.add(elem3);

    nestedForeach.process(eventBuilder().message(InternalMessage.of(payload)).build());
    assertNestedProcessedMessages();
  }

  @Test
  public void nestedArrayPayload() throws Exception {
    String[][] payload = new String[3][2];
    payload[0][0] = "a1";
    payload[0][1] = "a2";
    payload[1][0] = "a3";
    payload[1][1] = "b1";
    payload[2][0] = "b2";
    payload[2][1] = "c1";

    nestedForeach.process(eventBuilder().message(InternalMessage.of(payload)).build());
    assertNestedProcessedMessages();
  }

  @Test
  public void nestedMuleMessageCollectionPayload() throws Exception {

    List<InternalMessage> parentList = new ArrayList<>();
    List<InternalMessage> list1 = new ArrayList<>();
    List<InternalMessage> list2 = new ArrayList<>();

    list1.add(InternalMessage.builder().payload("a1").build());
    list1.add(InternalMessage.builder().payload("a2").build());
    list1.add(InternalMessage.builder().payload("a3").build());

    list2.add(InternalMessage.builder().payload("b1").build());
    list2.add(InternalMessage.builder().payload("b2").build());
    list2.add(InternalMessage.builder().payload("c1").build());

    parentList.add(InternalMessage.builder().payload(list1).build());
    parentList.add(InternalMessage.builder().payload(list2).build());
    InternalMessage parentCollection = InternalMessage.builder().payload(parentList).build();

    nestedForeach.process(eventBuilder().message(parentCollection).build());
    assertNestedProcessedMessages();
  }

  @Test
  public void nestedIterablePayload() throws Exception {
    Iterable iterable = new DummyNestedIterableClass();

    nestedForeach.process(eventBuilder().message(InternalMessage.of(iterable)).build());
    assertNestedProcessedMessages();
  }

  @Test
  public void nestedIteratorPayload() throws Exception {
    Iterable iterable = new DummyNestedIterableClass();

    nestedForeach.process(eventBuilder().message(InternalMessage.of(iterable.iterator())).build());
    assertNestedProcessedMessages();
  }

  @Test
  public void addProcessorPathElementsBeforeInit() throws MuleException {
    Foreach foreachMp = new Foreach();
    foreachMp.setMuleContext(muleContext);
    foreachMp.setFlowConstruct(mock(Flow.class));
    List<Processor> processors = getSimpleMessageProcessors();
    foreachMp.setMessageProcessors(processors);

    MessageProcessorPathElement parentElement = mock(MessageProcessorPathElement.class);
    MessageProcessorPathElement foreachElement = mock(MessageProcessorPathElement.class);
    when(parentElement.addChild(any(Processor.class))).thenReturn(foreachElement);
    foreachMp.addMessageProcessorPathElements(parentElement);

    assertAddedPathElements(processors, foreachElement);
  }

  protected void assertAddedPathElements(List<Processor> processors, MessageProcessorPathElement mpPathElement) {
    verify(mpPathElement, times(processors.size())).addChild(any(Processor.class));
    verify(mpPathElement).addChild(processors.get(0));
    verify(mpPathElement).addChild(processors.get(1));
  }

  private void assertSimpleProcessedMessages() {
    assertEquals(ERR_NUMBER_MESSAGES, 2, processedEvents.size());
    assertTrue(ERR_PAYLOAD_TYPE, processedEvents.get(0).getMessage().getPayload().getValue() instanceof String);
    assertTrue(ERR_PAYLOAD_TYPE, processedEvents.get(1).getMessage().getPayload().getValue() instanceof String);
    assertEquals(ERR_OUTPUT, "bar:foo:zas", processedEvents.get(0).getMessage().getPayload().getValue());
    assertEquals(ERR_OUTPUT, "zip:foo:zas", processedEvents.get(1).getMessage().getPayload().getValue());
  }

  private void assertNestedProcessedMessages() {
    String[] expectedOutputs = {"a1:foo:zas", "a2:foo:zas", "a3:foo:zas", "b1:foo:zas", "b2:foo:zas", "c1:foo:zas"};
    assertEquals(ERR_NUMBER_MESSAGES, 6, processedEvents.size());
    for (int i = 0; i < processedEvents.size(); i++) {
      assertTrue(ERR_PAYLOAD_TYPE, processedEvents.get(i).getMessage().getPayload().getValue() instanceof String);
    }
    for (int i = 0; i < processedEvents.size(); i++) {
      assertEquals(ERR_OUTPUT, expectedOutputs[i], processedEvents.get(i).getMessage().getPayload().getValue());
    }
  }

  public class DummySimpleIterableClass implements Iterable<String> {

    public List<String> strings = new ArrayList<>();

    public DummySimpleIterableClass() {
      strings.add("bar");
      strings.add("zip");
    }

    @Override
    public Iterator<String> iterator() {
      return strings.iterator();
    }
  }

  private class DummyNestedIterableClass implements Iterable<DummySimpleIterableClass> {

    private List<DummySimpleIterableClass> iterables = new ArrayList<>();

    public DummyNestedIterableClass() {
      DummySimpleIterableClass dsi1 = new DummySimpleIterableClass();
      dsi1.strings = new ArrayList<>();
      dsi1.strings.add("a1");
      dsi1.strings.add("a2");
      DummySimpleIterableClass dsi2 = new DummySimpleIterableClass();
      dsi2.strings = new ArrayList<>();
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
