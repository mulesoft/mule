/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static java.lang.Thread.currentThread;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.functional.junit4.TransactionConfigEnum.ACTION_NONE;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.CompositeMessageSource;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.source.StartableCompositeMessageSource;
import org.mule.runtime.core.transformer.simple.StringAppendTransformer;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.mule.TestTransactionFactory;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

public class FlowConfigurationFunctionalTestCase extends AbstractIntegrationTestCase {

  public FlowConfigurationFunctionalTestCase() {
    setDisposeContextPerClass(true);
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/flow.xml";
  }

  @Test
  public void testFlow() throws Exception {
    final Flow flow = muleContext.getRegistry().lookupObject("flow");
    assertEquals(5, flow.getMessageProcessors().size());
    assertNotNull(flow.getExceptionListener());

    assertEquals("012xyzabc3", getPayloadAsString(flowRunner("flow").withPayload("0").run().getMessage()));

  }

  @Test
  public void testFlowSynchronous() throws Exception {
    flowRunner("synchronousFlow").withPayload("0").run();
    InternalMessage message = muleContext.getClient().request("test://synchronous-out", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(message);
    Thread thread = (Thread) message.getPayload().getValue();
    assertNotNull(thread);
    assertEquals(Thread.currentThread(), thread);
  }

  @Test
  public void testFlowAynchronous() throws Exception {
    flowRunner("asynchronousFlow").withPayload("0").asynchronously().run();
    InternalMessage message = muleContext.getClient().request("test://asynchronous-out", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(message);
    Thread thread = (Thread) message.getPayload().getValue();
    assertNotNull(thread);
    assertNotSame(Thread.currentThread(), thread);
  }

  @Test
  public void testAsyncAsynchronous() throws Exception {
    flowRunner("asynchronousAsync").withPayload("0").asynchronously().run();
    InternalMessage message = muleContext.getClient().request("test://asynchronous-async-out", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(message);
    Thread thread = (Thread) message.getPayload().getValue();
    assertNotNull(thread);
    assertNotSame(Thread.currentThread(), thread);
  }

  @Test
  public void testFlowCompositeSource() throws Exception {
    final Flow flow = muleContext.getRegistry().lookupObject("flow2");
    CompositeMessageSource compositeSource = (CompositeMessageSource) flow.getMessageSource();
    assertEquals(StartableCompositeMessageSource.class, compositeSource.getClass());
    assertEquals(2, flow.getMessageProcessors().size());

    final List<MessageSource> sources = compositeSource.getSources();
    TestSimpleMessageSource source1 = (TestSimpleMessageSource) sources.get(0);
    TestSimpleMessageSource source2 = (TestSimpleMessageSource) sources.get(1);

    assertEquals("01xyz", getPayloadAsString(source1.fireEvent(getTestEvent("0")).getMessage()));
    assertEquals("01xyz", getPayloadAsString(source2.fireEvent(getTestEvent("0")).getMessage()));
  }

  @Test
  public void testInOutFlow() throws Exception {
    flowRunner("inout").withPayload("0").run();
    assertEquals("0", getPayloadAsString(muleContext.getClient().request("test://inout-out", RECEIVE_TIMEOUT).getRight().get()));
  }

  @Test
  public void testInOutAppendFlow() throws Exception {
    flowRunner("inout-append").withPayload("0").run();
    MuleClient client = muleContext.getClient();
    assertEquals("0inout", getPayloadAsString(client.request("test://inout-append-out", RECEIVE_TIMEOUT).getRight().get()));
  }

  @Test
  @Ignore("MULE-10184 - ArtifactClassLoaderRunner: groovy issue")
  public void testSplitAggregateFlow() throws Exception {
    final Apple apple = new Apple();
    final Banana banana = new Banana();
    final Orange orange = new Orange();
    final FruitBowl fruitBowl = new FruitBowl(apple, banana);
    fruitBowl.addFruit(orange);

    flowRunner("split-aggregate").withPayload(fruitBowl).run();

    final InternalMessage result =
        muleContext.getClient().request("test://split-aggregate-out", RECEIVE_TIMEOUT).getRight().get();

    assertNotNull(result);
    assertTrue(result.getPayload().getValue() instanceof List);
    final List<InternalMessage> coll = (List<InternalMessage>) result.getPayload().getValue();
    assertEquals(3, coll.size());
    final List<Fruit> results = coll.stream().map(msg -> (Fruit) msg.getPayload().getValue()).collect(toList());

    assertTrue(apple.isBitten());
    assertTrue(banana.isBitten());
    assertTrue(orange.isBitten());

    assertTrue(results.contains(apple));
    assertTrue(results.contains(banana));
    assertTrue(results.contains(orange));
  }

  @Test
  public void testSplitNoParts() throws Exception {
    String MESSAGE = "<Order></Order>";
    InternalMessage result = flowRunner("split-no-parts").withPayload(MESSAGE).run().getMessage();

    assertNotNull(result);
    assertEquals(result.getPayload().getValue(), MESSAGE);
  }

  @Test
  @Ignore("MULE-10184 - ArtifactClassLoaderRunner: groovy issue")
  public void testSplitAggregateListFlow() throws Exception {
    final Apple apple = new Apple();
    final Banana banana = new Banana();
    final Orange orange = new Orange();
    final FruitBowl fruitBowl = new FruitBowl(apple, banana);
    fruitBowl.addFruit(orange);

    flowRunner("split-aggregate-list").withPayload(fruitBowl.getFruit()).run();

    final InternalMessage result =
        muleContext.getClient().request("test://split-aggregate-list-out", RECEIVE_TIMEOUT).getRight().get();

    assertNotNull(result);
    assertTrue(result.getPayload().getValue() instanceof List);
    final List<InternalMessage> coll = (List<InternalMessage>) result.getPayload().getValue();
    assertEquals(3, coll.size());
    final List<Fruit> results = coll.stream().map(msg -> (Fruit) msg.getPayload().getValue()).collect(toList());

    assertTrue(apple.isBitten());
    assertTrue(banana.isBitten());
    assertTrue(orange.isBitten());

    assertTrue(results.contains(apple));
    assertTrue(results.contains(banana));
    assertTrue(results.contains(orange));
  }

  @Test
  @Ignore("MULE-10184 - ArtifactClassLoaderRunner: groovy issue")
  public void testSplitAggregateListFlowSingleItem() throws Exception {
    final Apple apple = new Apple();
    final FruitBowl fruitBowl = new FruitBowl();
    fruitBowl.addFruit(apple);

    flowRunner("split-aggregate-singleton-list").withPayload(fruitBowl.getFruit()).run();

    final MuleClient client = muleContext.getClient();
    final InternalMessage result = client.request("test://split-aggregate-singleton-list-out", RECEIVE_TIMEOUT).getRight().get();

    assertNotNull(result);
    assertTrue(result.getPayload().getValue() instanceof List);
    final List<InternalMessage> coll = (List<InternalMessage>) result.getPayload().getValue();
    assertEquals(1, coll.size());
    final List<Fruit> results = coll.stream().map(msg -> (Fruit) msg.getPayload().getValue()).collect(toList());

    assertTrue(apple.isBitten());

    assertTrue(results.contains(apple));
  }

  @Test
  @Ignore("MULE-10184 - ArtifactClassLoaderRunner: groovy issue")
  public void testSplitAggregateResponseListFlow() throws Exception {
    final Apple apple = new Apple();
    final Banana banana = new Banana();
    final Orange orange = new Orange();
    final FruitBowl fruitBowl = new FruitBowl(apple, banana);
    fruitBowl.addFruit(orange);

    final InternalMessage result =
        flowRunner("split-aggregate-response-list").withPayload(fruitBowl.getFruit()).run().getMessage();

    assertNotNull(result);
    assertTrue(result.getPayload().getValue() instanceof List);
    final List<InternalMessage> coll = (List<InternalMessage>) result.getPayload().getValue();
    assertEquals(3, coll.size());
    final List<Fruit> results = coll.stream().map(msg -> (Fruit) msg.getPayload().getValue()).collect(toList());

    assertTrue(apple.isBitten());
    assertTrue(banana.isBitten());
    assertTrue(orange.isBitten());

    assertTrue(results.contains(apple));
    assertTrue(results.contains(banana));
    assertTrue(results.contains(orange));
  }

  @Test
  @Ignore("MULE-10184 - ArtifactClassLoaderRunner: groovy issue")
  public void testSplitAggregateResponseListFlowSingleItem() throws Exception {
    final Apple apple = new Apple();
    final FruitBowl fruitBowl = new FruitBowl();
    fruitBowl.addFruit(apple);

    final InternalMessage result =
        flowRunner("split-aggregate-response-singleton-list").withPayload(fruitBowl.getFruit()).run().getMessage();

    assertNotNull(result);
    assertTrue(result.getPayload().getValue() instanceof List);
    final List<InternalMessage> coll = (List<InternalMessage>) result.getPayload().getValue();
    assertEquals(1, coll.size());
    final List<Fruit> results = coll.stream().map(msg -> (Fruit) msg.getPayload().getValue()).collect(toList());

    assertTrue(apple.isBitten());
    assertTrue(results.contains(apple));
  }

  @Test
  @Ignore("MULE-10184 - ArtifactClassLoaderRunner: groovy issue")
  public void testSplitAggregateMapFlow() throws Exception {
    Map<String, Fruit> map = new HashMap<>();
    final Apple apple = new Apple();
    final Banana banana = new Banana();
    final Orange orange = new Orange();
    map.put("apple", apple);
    map.put("banana", banana);
    map.put("orange", orange);

    Event result = flowRunner("split-aggregate-map").withPayload(map).run();

    assertNotNull(result);
    assertTrue(result.getMessage().getPayload().getValue() instanceof List);
    final InternalMessage[] results = new InternalMessage[3];
    ((List<InternalMessage>) result.getMessage().getPayload().getValue()).toArray(results);
    assertEquals(3, results.length);

    assertTrue(apple.isBitten());
    assertTrue(banana.isBitten());
    assertTrue(orange.isBitten());
  }

  @Test
  public void testSplitFilterAggregateFlow() throws Exception {
    final Apple apple = new Apple();
    final Banana banana = new Banana();
    final Orange orange = new Orange();
    final FruitBowl fruitBowl = new FruitBowl(apple, banana);
    fruitBowl.addFruit(orange);

    flowRunner("split-filter-aggregate").withPayload(fruitBowl).run();

    final MuleClient client = muleContext.getClient();
    final InternalMessage result = client.request("test://split-filter-aggregate-out", RECEIVE_TIMEOUT).getRight().get();

    assertNotNull(result);
    assertTrue(result.getPayload().getValue() instanceof List);
    final List<InternalMessage> coll = (List<InternalMessage>) result.getPayload().getValue();
    assertEquals(1, coll.size());
    final List<Fruit> results = coll.stream().map(msg -> (Fruit) msg.getPayload().getValue()).collect(toList());

    assertTrue(results.contains(apple));
    assertFalse(results.contains(banana));
    assertFalse(results.contains(orange));
  }

  @Test
  public void testMessageChunkSplitAggregateFlow() throws Exception {
    String payload = "";
    for (int i = 0; i < 100; i++) {
      payload += TEST_MESSAGE;
    }

    flowRunner("message-chunk-split-aggregate").withPayload(payload).run();

    MuleClient client = muleContext.getClient();
    final InternalMessage result = client.request("test://message-chunk-split-aggregate-out", RECEIVE_TIMEOUT).getRight().get();

    assertNotNull(result);
    assertNotSame(payload, result.getPayload().getValue());
    assertEquals(payload, getPayloadAsString(result));
  }

  @Test
  public void testComponentsFlow() throws Exception {
    final InternalMessage result = flowRunner("components").withPayload("0").run().getMessage();

    assertNotNull(result);
    assertNotSame(TEST_MESSAGE + "test", result.getPayload().getValue());
  }

  @Test
  public void testWireTapFlow() throws Exception {
    flowRunner("wiretap").withPayload(TEST_MESSAGE).run();

    final MuleClient client = muleContext.getClient();
    final InternalMessage result = client.request("test://wiretap-out", RECEIVE_TIMEOUT).getRight().get();
    final InternalMessage tapResult = client.request("test://wiretap-tap", RECEIVE_TIMEOUT).getRight().get();

    assertNotNull(result);
    assertNotNull(tapResult);
    assertNotSame(result, tapResult);
    assertEquals(TEST_MESSAGE + "inout", getPayloadAsString(result));
    assertEquals(TEST_MESSAGE + "intap", getPayloadAsString(tapResult));
  }

  @Test
  public void testResponseElement() throws Exception {
    final InternalMessage result = flowRunner("response").withPayload("").run().getMessage();

    assertNotNull(result);
    assertEquals("abcdefghi", getPayloadAsString(result));
  }

  @Test
  public void testAsyncOneWayEndpoint() throws Exception {
    flowRunner("async-oneway").withPayload("0").run();
    MuleClient client = muleContext.getClient();
    final InternalMessage result = client.request("test://async-oneway-out", RECEIVE_TIMEOUT).getRight().get();
    final InternalMessage asyncResult = client.request("test://async-async-oneway-out", RECEIVE_TIMEOUT).getRight().get();

    assertNotNull(result);
    assertNotNull(asyncResult);
    assertEquals("0ac", getPayloadAsString(result));
    assertEquals("0ab", getPayloadAsString(asyncResult));
  }

  @Test
  public void testAsyncRequestResponseEndpoint() throws Exception {
    flowRunner("async-requestresponse").withPayload("0").run();
    MuleClient client = muleContext.getClient();
    final InternalMessage result = client.request("test://async-requestresponse-out", RECEIVE_TIMEOUT).getRight().get();
    final InternalMessage asyncResult =
        client.request("test://async-async-requestresponse-out", RECEIVE_TIMEOUT).getRight().get();

    assertNotNull(result);
    assertNotNull(asyncResult);
    assertEquals("0ac", getPayloadAsString(result));
    assertEquals("0ab", getPayloadAsString(asyncResult));
  }

  @Test
  public void testAsyncTransactionalEndpoint() throws Exception {
    Exception e = flowRunner("async-tx").withPayload("0").transactionally(ACTION_NONE, new TestTransactionFactory())
        .asynchronously().runExpectingException();

    assertThat(e, instanceOf(MessagingException.class));
    assertThat(e.getMessage(), containsString("The <async> element cannot be used with transactions"));

    final MuleClient client = muleContext.getClient();
    assertThat(client.request("test://async-requestresponse-out", RECEIVE_TIMEOUT).getRight().isPresent(), is(false));
    assertThat(client.request("test://async-async-oneway-out", RECEIVE_TIMEOUT).getRight().isPresent(), is(false));
  }

  @Test
  public void testMulticaster() throws Exception {
    flowRunner("multicaster").withPayload(TEST_MESSAGE).run();

    final MuleClient client = muleContext.getClient();
    final InternalMessage result1 = client.request("test://multicaster-out1", RECEIVE_TIMEOUT).getRight().get();
    final InternalMessage result2 = client.request("test://multicaster-out2", RECEIVE_TIMEOUT).getRight().get();
    final InternalMessage result3 = client.request("test://multicaster-out3", RECEIVE_TIMEOUT).getRight().get();

    assertNotNull(result1);
    assertNotNull(result2);
    assertNotNull(result3);

    assertEquals(TEST_MESSAGE, result1.getPayload().getValue());
    assertEquals(TEST_MESSAGE, result1.getPayload().getValue());
    assertEquals(TEST_MESSAGE, result1.getPayload().getValue());

  }

  @Test
  public void testChoiceWithoutOutboundEndpoints() throws Exception {
    assertEquals("foo Hello foo", getPayloadAsString(flowRunner("choice2").withPayload("foo").run().getMessage()));
    assertEquals("bar Hello bar", getPayloadAsString(flowRunner("choice2").withPayload("bar").run().getMessage()));
    assertEquals("egh Hello ?", getPayloadAsString(flowRunner("choice2").withPayload("egh").run().getMessage()));
  }

  @Test
  public void testFlowRef() throws Exception {
    final InternalMessage message = flowRunner("flow-ref").withPayload("0").run().getMessage();
    assertEquals("012xyzabc312xyzabc3", getPayloadAsString(message));
  }

  @Test
  public void testInvoke() throws Exception {
    final InternalMessage message = flowRunner("invoke").withPayload("0").run().getMessage();
    assertEquals("0recieved", getPayloadAsString(message));
  }

  @Test
  public void testInvoke2() throws Exception {
    final InternalMessage response =
        flowRunner("invoke2").withPayload("0").withInboundProperty("one", "header1val").run().getMessage();
    assertEquals("header1valrecieved", getPayloadAsString(response));
  }

  @Test
  public void testInvoke3() throws Exception {
    // ensure multiple arguments work
    flowRunner("invoke3").withPayload("0").run();
  }

  @Test
  public void testInvoke4() throws Exception {
    // ensure no arguments work
    flowRunner("invoke4").withPayload("0").run();
  }

  @Test
  public void testEnrichWithAttributes() throws Exception {
    final InternalMessage muleMessage = flowRunner("enrich").withPayload("0").run().getMessage();
    assertEquals("0Hello", muleMessage.getOutboundProperty("helloHeader"));
  }

  @Test
  public void testEnrichWithElements() throws Exception {
    InternalMessage result = flowRunner("enrich2").withPayload("0").run().getMessage();

    assertEquals("0Hello", result.getOutboundProperty("helloHeader"));
    assertEquals("0Hello", result.getOutboundProperty("helloHeader2"));
  }

  @Test
  public void testEnrichUsingComponent() throws Exception {
    // MULE-5544
    InternalMessage result = flowRunner("enrichcomponent").withPayload("0").run().getMessage();

    assertEquals("0", result.getOutboundProperty("echoHeader"));
  }

  @Test
  public void testEnrichUsingComponent2() throws Exception {
    // MULE-5544
    InternalMessage result = flowRunner("enrichcomponent2").withPayload("0").run().getMessage();

    assertEquals("0", result.getOutboundProperty("echoHeader"));
  }

  @Test
  public void testLoggerMessage() throws Exception {
    flowRunner("loggermessage").withPayload("0").run();
  }

  @Test
  public void testLoggerHeader() throws Exception {
    flowRunner("loggerheader").withPayload("0").withOutboundProperty("toLog", "valueToLog").run();
  }

  public static class Pojo {

    public void method() {
      // does nothing
    }

    public void method(Object arg1, Object arg2) {
      // does nothing
    }
  }

  @Test
  public void testCustomMessageRouter() throws Exception {
    InternalMessage result = flowRunner("customRouter").withPayload("").run().getMessage();
    assertEquals("abc",
                 ((List<InternalMessage>) result.getPayload().getValue()).stream()
                     .map(msg -> (String) msg.getPayload().getValue()).collect(joining()));
  }

  @Test
  public void testPoll() throws Exception {
    InternalMessage message = muleContext.getClient().request("test://poll-out", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(message);
    assertEquals(" Hello fooout", getPayloadAsString(message));
  }

  @Test
  public void testPollFlowRef() throws Exception {
    InternalMessage message = muleContext.getClient().request("test://poll2-out", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(message);
    assertEquals("pollappendout", getPayloadAsString(message));
  }

  @Test
  public void testSubFlowMessageFilter() throws Exception {
    flowRunner("messagefiltersubflow").withPayload("0").asynchronously().run();
    InternalMessage message =
        muleContext.getClient().request("test://messagefiltersubflow-out", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(message);
  }

  @Test
  public void testCustomMessageSource() throws Exception {
    Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("customMessageSource");
    TestMessageSource source = (TestMessageSource) flow.getMessageSource();

    Event result = source.fireEvent(getTestEvent("a"));
    assertEquals("abcd", result.getMessageAsString(muleContext));
  }

  @Test
  public void testCustomMessageSourceInComposite() throws Exception {
    Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("customMessageSourceInComposite");
    CompositeMessageSource compositeSource = (CompositeMessageSource) flow.getMessageSource();
    TestMessageSource source = (TestMessageSource) compositeSource.getSources().get(0);

    Event result = source.fireEvent(getTestEvent("a"));
    assertEquals("abcd", result.getMessageAsString(muleContext));
  }


  public static class TestMessageSource implements MessageSource {

    private Processor listener;
    private String appendBefore;
    private String appendAfter;

    Event fireEvent(Event event) throws MuleException {
      Transformer before = new StringAppendTransformer(appendBefore);
      Transformer after = new StringAppendTransformer(appendAfter);
      before.setMuleContext(muleContext);
      after.setMuleContext(muleContext);
      return after.process(listener.process(before.process(event)));
    }

    public void setAppendBefore(String appendBefore) {
      this.appendBefore = appendBefore;
    }

    public void setAppendAfter(String appendAfter) {
      this.appendAfter = appendAfter;
    }

    @Override
    public void setListener(Processor listener) {
      this.listener = listener;
    }

  }

  public static class TestSimpleMessageSource implements MessageSource {

    private Processor listener;


    Event fireEvent(Event event) throws MuleException {
      return listener.process(event);
    }

    @Override
    public void setListener(Processor listener) {
      this.listener = listener;
    }

  }

  public static class ThreadSensingMessageProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      return Event.builder(event).message(InternalMessage.builder(event.getMessage()).payload(currentThread()).build()).build();
    }
  }

}
