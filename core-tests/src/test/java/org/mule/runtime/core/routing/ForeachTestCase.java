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

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
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
  protected ArrayList<MuleEvent> processedEvents;

  private static String ERR_NUMBER_MESSAGES = "Not a correct number of messages processed";
  private static String ERR_PAYLOAD_TYPE = "Type error on processed payloads";
  private static String ERR_OUTPUT = "Messages processed incorrectly";

  @Before
  public void initialise() throws MuleException {
    processedEvents = new ArrayList<>();
    simpleForeach = createForeach(getSimpleMessageProcessors());
    nestedForeach = createForeach(getNestedMessageProcessors());
  }

  private List<MessageProcessor> getSimpleMessageProcessors() {
    List<MessageProcessor> lmp = new ArrayList<>();
    lmp.add(event -> {
      String payload = event.getMessage().getPayload().toString();
      event.setMessage(MuleMessage.builder(event.getMessage()).payload(payload + ":foo").build());
      processedEvents.add(event);
      return event;
    });
    lmp.add(new TestMessageProcessor("zas"));
    return lmp;
  }

  private List<MessageProcessor> getNestedMessageProcessors() throws MuleException {
    List<MessageProcessor> lmp = new ArrayList<>();
    Foreach internalForeach = new Foreach();
    internalForeach.setMessageProcessors(getSimpleMessageProcessors());
    lmp.add(internalForeach);
    return lmp;
  }

  private Foreach createForeach(List<MessageProcessor> mps) throws MuleException {
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
    simpleForeach.process(getTestEvent(arrayList));

    assertSimpleProcessedMessages();
  }

  @Test
  public void arrayPayload() throws Exception {
    String[] array = new String[2];
    array[0] = "bar";
    array[1] = "zip";
    simpleForeach.process(getTestEvent(array));

    assertSimpleProcessedMessages();
  }

  @Test
  public void muleMessageCollectionPayload() throws Exception {
    List<MuleMessage> list = new ArrayList<>();
    list.add(MuleMessage.builder().payload("bar").build());
    list.add(MuleMessage.builder().payload("zip").build());
    MuleMessage msgCollection = MuleMessage.builder().payload(list).build();
    simpleForeach.process(getTestEvent(msgCollection));

    assertSimpleProcessedMessages();
  }

  @Test
  public void iterablePayload() throws Exception {
    Iterable<String> iterable = new DummySimpleIterableClass();
    simpleForeach.process(getTestEvent(iterable));

    assertSimpleProcessedMessages();
  }

  @Test
  public void iteratorPayload() throws Exception {
    Iterable<String> iterable = new DummySimpleIterableClass();
    simpleForeach.process(getTestEvent(iterable.iterator()));

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

    nestedForeach.process(getTestEvent(payload));
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

    nestedForeach.process(getTestEvent(payload));
    assertNestedProcessedMessages();
  }

  @Test
  public void nestedMuleMessageCollectionPayload() throws Exception {

    List<MuleMessage> parentList = new ArrayList<>();
    List<MuleMessage> list1 = new ArrayList<>();
    List<MuleMessage> list2 = new ArrayList<>();

    list1.add(MuleMessage.builder().payload("a1").build());
    list1.add(MuleMessage.builder().payload("a2").build());
    list1.add(MuleMessage.builder().payload("a3").build());

    list2.add(MuleMessage.builder().payload("b1").build());
    list2.add(MuleMessage.builder().payload("b2").build());
    list2.add(MuleMessage.builder().payload("c1").build());

    parentList.add(MuleMessage.builder().payload(list1).build());
    parentList.add(MuleMessage.builder().payload(list2).build());
    MuleMessage parentCollection = MuleMessage.builder().payload(parentList).build();

    nestedForeach.process(getTestEvent(parentCollection));
    assertNestedProcessedMessages();
  }

  @Test
  public void nestedIterablePayload() throws Exception {
    Iterable iterable = new DummyNestedIterableClass();

    nestedForeach.process(getTestEvent(iterable));
    assertNestedProcessedMessages();
  }

  @Test
  public void nestedIteratorPayload() throws Exception {
    Iterable iterable = new DummyNestedIterableClass();

    nestedForeach.process(getTestEvent(iterable.iterator()));
    assertNestedProcessedMessages();
  }

  @Test
  public void addProcessorPathElementsBeforeInit() throws MuleException {
    Foreach foreachMp = new Foreach();
    foreachMp.setMuleContext(muleContext);
    List<MessageProcessor> processors = getSimpleMessageProcessors();
    foreachMp.setMessageProcessors(processors);

    MessageProcessorPathElement mpPathElement = mock(MessageProcessorPathElement.class);
    foreachMp.addMessageProcessorPathElements(mpPathElement);

    assertAddedPathElements(processors, mpPathElement);
  }

  @Test
  public void addProcessorPathElementsAfterInit() throws MuleException {
    Foreach foreachMp = new Foreach();
    foreachMp.setMuleContext(muleContext);
    List<MessageProcessor> processors = getSimpleMessageProcessors();
    foreachMp.setMessageProcessors(processors);
    foreachMp.initialise();

    MessageProcessorPathElement mpPathElement = mock(MessageProcessorPathElement.class);
    foreachMp.addMessageProcessorPathElements(mpPathElement);

    // Remove MPs added by the foreach as it does not copies the list
    final List<MessageProcessor> originalMessageProcessors = processors.subList(1, 3);
    assertAddedPathElements(originalMessageProcessors, mpPathElement);
  }

  protected void assertAddedPathElements(List<MessageProcessor> processors, MessageProcessorPathElement mpPathElement) {
    verify(mpPathElement, times(processors.size())).addChild(any(MessageProcessor.class));
    verify(mpPathElement).addChild(processors.get(0));
    verify(mpPathElement).addChild(processors.get(1));
  }

  private void assertSimpleProcessedMessages() {
    assertEquals(ERR_NUMBER_MESSAGES, 2, processedEvents.size());
    assertTrue(ERR_PAYLOAD_TYPE, processedEvents.get(0).getMessage().getPayload() instanceof String);
    assertTrue(ERR_PAYLOAD_TYPE, processedEvents.get(1).getMessage().getPayload() instanceof String);
    assertEquals(ERR_OUTPUT, "bar:foo:zas", processedEvents.get(0).getMessage().getPayload());
    assertEquals(ERR_OUTPUT, "zip:foo:zas", processedEvents.get(1).getMessage().getPayload());
  }

  private void assertNestedProcessedMessages() {
    String[] expectedOutputs = {"a1:foo:zas", "a2:foo:zas", "a3:foo:zas", "b1:foo:zas", "b2:foo:zas", "c1:foo:zas"};
    assertEquals(ERR_NUMBER_MESSAGES, 6, processedEvents.size());
    for (int i = 0; i < processedEvents.size(); i++) {
      assertTrue(ERR_PAYLOAD_TYPE, processedEvents.get(i).getMessage().getPayload() instanceof String);
    }
    for (int i = 0; i < processedEvents.size(); i++) {
      assertEquals(ERR_OUTPUT, expectedOutputs[i], processedEvents.get(i).getMessage().getPayload());
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
