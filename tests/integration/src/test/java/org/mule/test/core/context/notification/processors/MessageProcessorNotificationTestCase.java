/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification.processors;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.mule.runtime.api.message.Message.of;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.CompositeMessageSource;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.component.ComponentException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.test.core.context.notification.Node;
import org.mule.test.core.context.notification.RestrictedNode;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.Factory;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MessageProcessorNotificationTestCase extends AbstractMessageProcessorNotificationTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/notifications/message-processor-notification-test-flow.xml";
  }

  private Factory specificationFactory;

  @Test
  public void single() throws Exception {
    specificationFactory = () -> new Node()
        .serial(prePost());

    assertNotNull(flowRunner("singleMP").withPayload(TEST_PAYLOAD).run());

    assertNotifications();
  }

  @Ignore //TODO remove ignore with final commit for MULE-11482
  @Test
  public void chain() throws Exception {
    specificationFactory = () -> new Node()
        .serial(pre()) // Message Processor Chain
        .serial(pre()) // // collection-aggregator
        .serial(prePost()) // logger-1
        .serial(prePost()) // logger-2
        .serial(post()) // collection-aggregator
        .serial(post()) // Message Processor Chain
    ;

    assertNotNull(flowRunner("processorChain").withPayload(TEST_PAYLOAD).run());

    assertNotifications();
  }

  @Test
  public void customProcessor() throws Exception {
    specificationFactory = () -> new Node()
        .serial(prePost())
        .serial(prePost());

    assertNotNull(flowRunner("customProcessor").withPayload(TEST_PAYLOAD).run());

    assertNotifications();
  }

  @Test
  public void choice() throws Exception {
    specificationFactory = () -> new Node()
        // choice
        .serial(pre()) // choice
        .serial(prePost()) // otherwise-logger
        .serial(post());

    assertNotNull(flowRunner("choice").withPayload(TEST_PAYLOAD).run());

    assertNotifications();
  }

  @Ignore //TODO remove ignore with final commit for MULE-11482
  @Test
  public void scatterGather() throws Exception {
    specificationFactory = () -> new Node()
        .serial(pre()) // scatter-gather
        .serial(new Node()
            .parallelSynch(pre() // route 1 chain
                .serial(prePost()) // route 1 first logger
                .serial(prePost()) // route 1 second logger
                .serial(post())) // route 1 chain
            .parallelSynch(pre() // route 2 chain
                .serial(prePost()) // route 2 logger
                .serial(post()))) // route 2 chain
        .serial(post()) // scatter-gather
    ;

    assertNotNull(flowRunner("scatterGather").withPayload(TEST_PAYLOAD).run());

    assertNotifications();
  }

  @Test
  public void foreach() throws Exception {
    specificationFactory = () -> new Node()
        .serial(pre()) // foreach
        .serial(prePost()) // logger-loop-1
        .serial(prePost()) // logger-loop-2
        .serial(post())
        .serial(prePost()) // MP after the Scope
    ;

    assertNotNull(flowRunner("foreach").withPayload(TEST_PAYLOAD).run());

    assertNotifications();
  }

  @Ignore //TODO remove ignore with final commit for MULE-11482
  @Test
  public void enricher() throws Exception {
    specificationFactory = () -> new Node()
        .serial(pre())
        .serial(prePost()) // append-string
        .serial(post())
        .serial(pre())
        .serial(pre()) // chain
        .serial(prePost()) // echo
        .serial(prePost()) // echo
        .serial(post()) // chain
        .serial(post());

    assertNotNull(flowRunner("enricher").withPayload(TEST_PAYLOAD).run());

    assertNotifications();
  }

  @Test
  @Ignore("This is unstable")
  public void async() throws Exception {
    specificationFactory = () -> new Node()
        .serial(prePost())
        .serial(prePost())
        .serial(prePost());

    assertNotNull(flowRunner("in-async").withPayload(TEST_PAYLOAD).run());

    assertNotifications();
  }

  @Ignore //TODO remove ignore with final commit for MULE-11482
  @Test
  public void filter() throws Exception {
    specificationFactory = () -> new Node()
        .serial(pre())
        .serial(prePost())
        .serial(post());

    assertNotNull(flowRunner("filters").withPayload(TEST_PAYLOAD).run());

    assertNotifications();
  }

  @Ignore //TODO remove ignore with final commit for MULE-11482
  @Test
  public void idempotentMessageFilter() throws Exception {
    specificationFactory = () -> new Node()
        .serial(pre()) // open message filter
        .serial(prePost()) // message processor
        .serial(post()) // close mf
    ;

    assertNotNull(flowRunner("idempotent-msg-filter").withPayload(TEST_PAYLOAD).run());

    assertNotifications();
  }

  @Ignore //TODO remove ignore with final commit for MULE-11482
  @Test
  public void idempotentSecureHashMessageFilter() throws Exception {
    specificationFactory = () -> new Node()
        .serial(pre()) // open message filter
        .serial(prePost()) // message processor
        .serial(post()) // close mf
    ;

    assertNotNull(flowRunner("idempotent-secure-hash-msg-filter").withPayload(TEST_PAYLOAD).run());

    assertNotifications();
  }

  @Ignore //TODO remove ignore with final commit for MULE-11482
  @Test
  public void subFlow() throws Exception {
    specificationFactory = () -> new Node()
        .serial(prePost())
        .serial(pre())
        .serial(pre())
        .serial(prePost())
        .serial(post())
        .serial(post());

    assertNotNull(flowRunner("subflow").withPayload(TEST_PAYLOAD).run());

    assertNotifications();
  }

  @Test
  public void catchExceptionStrategy() throws Exception {
    specificationFactory = () -> new Node()
        // catch-es
        .serial(prePost())
        .serial(prePost());

    assertNotNull(flowRunner("catch-es").withPayload(TEST_PAYLOAD).run());

    assertNotifications();
  }

  @Test
  public void rollbackExceptionStrategy() throws Exception {
    specificationFactory = () -> new Node()
        // rollback-es
        .serial(prePost())
        .serial(prePost());

    expectedException.expect(MessagingException.class);
    expectedException.expectCause(instanceOf(ComponentException.class));
    flowRunner("rollback-es").withPayload(TEST_PAYLOAD).run();

    assertNotifications();
  }

  @Test
  public void choiceExceptionStrategy() throws Exception {
    specificationFactory = () -> new Node()
        .serial(prePost())
        .serial(prePost());

    assertNotNull(flowRunner("choice-es").withPayload(TEST_PAYLOAD).run());

    assertNotifications();
  }

  @Test
  public void compositeSource() throws Exception {
    specificationFactory = () -> new Node()
        .serial(prePost()) // call throw cs1
        .serial(prePost());

    final Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("composite-source");
    CompositeMessageSource composite = (CompositeMessageSource) flow.getMessageSource();
    assertNotNull(((TestMessageSource) composite.getSources().get(0))
        .fireEvent(Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR)).message(of(TEST_PAYLOAD)).build()));
    assertNotNull(((TestMessageSource) composite.getSources().get(1))
        .fireEvent(Event.builder(DefaultEventContext.create(flow, TEST_CONNECTOR)).message(of(TEST_PAYLOAD)).build()));

    assertNotifications();
  }

  @Test
  public void firstSuccessful() throws Exception {
    specificationFactory = () -> new Node()
        .serial(prePost()) // logger
        .serial(pre()) // first-successful
        .serial(prePost())
        .serial(prePost())
        .serial(prePost())
        .serial(prePost()) // dlq
        .serial(post());

    assertNotNull(flowRunner("first-successful").withPayload(TEST_PAYLOAD).run());

    assertNotifications();
  }

  @Test
  public void roundRobin() throws Exception {
    specificationFactory = () -> new Node()
        .serial(pre()) // round-robin
        .serial(prePost()) // inner logger
        .serial(post())
        .serial(prePost()) // logger
    ;

    assertNotNull(flowRunner("round-robin").withPayload(TEST_PAYLOAD).run());

    assertNotifications();
  }

  @Test
  public void collectionAggregator() throws Exception {
    specificationFactory = () -> new Node()
        .serial(pre()) // open Splitter, unpacks three messages
        .serial(prePost()) // 1st message on Logger
        .serial(prePost()) // gets to Aggregator
        .serial(prePost()) // 2nd message on Logger
        .serial(prePost()) // gets to Aggregator
        .serial(prePost()) // 3rd message on Logger
        .serial(prePost()) // gets to Aggregator and packs the three messages, then close
        .serial(post()) // close Splitter
    ;

    List<String> testList = Arrays.asList("test", "with", "collection");
    assertNotNull(flowRunner("collectionAggregator").withPayload(testList).run());

    assertNotifications();
  }

  @Test
  public void chunkAggregator() throws Exception {
    specificationFactory = () -> new Node()
        .serial(pre()) // start Splitter
        .serial(prePost()) // 1st message on Logger
        .serial(prePost()) // gets to Aggregator
        .serial(prePost()) // 2nd message on Logger
        .serial(prePost()) // gets to Aggregator
        .serial(prePost()) // 3rd message on Logger
        .serial(prePost()) // gets to Aggregator
        .serial(prePost()) // 4th message on Logger
        .serial(pre()) // gets to Aggregator and packs four messages
        .serial(prePost()) // packed message get to the second Logger
        .serial(post()) // close Aggregator
        .serial(post()) // close Splitter
    ;

    assertNotNull(flowRunner("chunkAggregator").withPayload(TEST_PAYLOAD).run());

    assertNotifications();
  }

  @Test
  public void wireTap() throws Exception {
    specificationFactory = () -> new Node()
        .serial(prePost())
        .serial(prePost());

    assertNotNull(flowRunner("wire-tap").withPayload(TEST_PAYLOAD).run());

    assertNotifications();
  }

  @Test
  public void untilSuccesful() throws Exception {
    specificationFactory = () -> new Node()
        .serial(pre())
        .serial(new Node()
            .parallelSynch(prePost())
            .parallelSynch(post().serial(prePost())));

    assertNotNull(flowRunner("until-successful").withPayload(TEST_PAYLOAD).run());
    muleContext.getClient().request("test://out-us", getTestTimeoutSecs()).getRight().get();

    assertNotifications();
  }

  @Ignore //TODO remove ignore with final commit for MULE-11482
  @Test
  public void untilSuccesfulWithProcessorChain() throws Exception {
    specificationFactory = () -> new Node()
        .serial(pre())
        .serial(new Node()
            .parallelSynch(pre().serial(prePost()).serial(prePost()).serial(post()))
            .parallelSynch(post().serial(prePost())));

    assertNotNull(flowRunner("until-successful-with-processor-chain").withPayload(TEST_PAYLOAD).run());
    muleContext.getClient().request("test://out-us", getTestTimeoutSecs()).getRight().get();

    assertNotifications();
  }

  @Ignore //TODO remove ignore with final commit for MULE-11482
  @Test
  public void untilSuccesfulWithEnricher() throws Exception {
    specificationFactory = () -> new Node()
        .serial(pre())
        .serial(new Node()
            .parallelSynch(pre().serial(pre()).serial(prePost()).serial(post()).serial(prePost()).serial(post()))
            .parallelSynch(post().serial(prePost())));

    assertNotNull(flowRunner("until-successful-with-enricher").withPayload(TEST_PAYLOAD).run());
    muleContext.getClient().request("test://out-us", getTestTimeoutSecs()).getRight().get();

    assertNotifications();
  }

  @Override
  public RestrictedNode getSpecification() {
    return (RestrictedNode) specificationFactory.create();
  }

  @Override
  public void validateSpecification(RestrictedNode spec) throws Exception {}

  public static class TestMessageSource implements MessageSource {

    private Processor listener;

    Event fireEvent(Event event) throws MuleException {
      return listener.process(event);
    }

    @Override
    public void setListener(Processor listener) {
      this.listener = listener;
    }

  }

}
