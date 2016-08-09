/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification.processors;

import static org.junit.Assert.assertNotNull;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.source.CompositeMessageSource;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.component.ComponentException;
import org.mule.runtime.core.construct.Flow;
import org.mule.test.core.context.notification.Node;
import org.mule.test.core.context.notification.RestrictedNode;

import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

@Ignore("MULE-10185 - ArtifactClassLoaderRunner CXF issue when running all tests, works when executed isolated")
public class MessageProcessorNotificationTestCase extends AbstractMessageProcessorNotificationTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/notifications/message-processor-notification-test-flow.xml";
  }

  @Override
  public void doTest() throws Exception {
    List<String> testList = Arrays.asList("test", "with", "collection");
    assertNotNull(flowRunner("singleMP").withPayload(TEST_PAYLOAD).run());
    assertNotNull(flowRunner("processorChain").withPayload(TEST_PAYLOAD).run());
    assertNotNull(flowRunner("customProcessor").withPayload(TEST_PAYLOAD).run());
    assertNotNull(flowRunner("choice").withPayload(TEST_PAYLOAD).run());
    assertNotNull(flowRunner("scatterGather").withPayload(TEST_PAYLOAD).run());

    assertNotNull(flowRunner("foreach").withPayload(TEST_PAYLOAD).run());
    assertNotNull(flowRunner("enricher").withPayload(TEST_PAYLOAD).run());
    // assertNotNull(runFlow("in-async", TEST_PAYLOAD));
    assertNotNull(flowRunner("filters").withPayload(TEST_PAYLOAD).run());
    assertNotNull(flowRunner("idempotent-msg-filter").withPayload(TEST_PAYLOAD).run());
    assertNotNull(flowRunner("idempotent-secure-hash-msg-filter").withPayload(TEST_PAYLOAD).run());
    assertNotNull(flowRunner("subflow").withPayload(TEST_PAYLOAD).run());
    assertNotNull(flowRunner("catch-es").withPayload(TEST_PAYLOAD).run());
    expectedException.expect(ComponentException.class);
    flowRunner("rollback-es").withPayload(TEST_PAYLOAD).run();
    assertNotNull(flowRunner("choice-es").withPayload(TEST_PAYLOAD).run());
    CompositeMessageSource composite =
        (CompositeMessageSource) ((Flow) muleContext.getRegistry().lookupFlowConstruct("composite-source")).getMessageSource();
    assertNotNull(((TestMessageSource) composite.getSources().get(0)).fireEvent(getTestEvent(TEST_PAYLOAD)));
    assertNotNull(((TestMessageSource) composite.getSources().get(1)).fireEvent(getTestEvent(TEST_PAYLOAD)));
    assertNotNull(flowRunner("first-successful").withPayload(TEST_PAYLOAD).run());
    assertNotNull(flowRunner("round-robin").withPayload(TEST_PAYLOAD).run());
    assertNotNull(flowRunner("collectionAggregator").withPayload(testList).run());
    assertNotNull(flowRunner("customAggregator").withPayload(testList).run());
    assertNotNull(flowRunner("chunkAggregator").withPayload(TEST_PAYLOAD).run());
    assertNotNull(flowRunner("wire-tap").withPayload(TEST_PAYLOAD).run());
    assertNotNull(flowRunner("until-successful").withPayload(TEST_PAYLOAD).run());
    assertNotNull(flowRunner("until-successful-with-processor-chain").withPayload(TEST_PAYLOAD).run());
    assertNotNull(flowRunner("until-successful-with-enricher").withPayload(TEST_PAYLOAD).run());
  }

  @Override
  public RestrictedNode getSpecification() {
    return new Node()
        // singleMP
        .serial(prePost())

        // processorChain
        .serial(pre()) // Message Processor Chain
        .serial(prePost()) // logger-1
        .serial(prePost()) // logger-2
        .serial(post()) // Message Processor Chain

        // custom-processor
        .serial(prePost()).serial(prePost())

        // choice
        .serial(pre()) // choice
        .serial(prePost()) // otherwise-logger
        .serial(post())

        // scatter-gather
        .serial(pre()) // scatter-gather
        .serial(new Node()
            .parallel(pre() // route 1 chain
                .serial(prePost()) // route 1 first logger
                .serial(prePost()) // route 1 second logger
                .serial(post())) // route 1 chain
            .parallel(prePost())) // route 0 logger
        .serial(post()) // scatter-gather

        // foreach
        .serial(pre()) // foreach
        .serial(prePost()) // logger-loop-1
        .serial(prePost()) // logger-loop-2
        .serial(post()).serial(prePost()) // MP after the Scope

        // enricher
        .serial(pre()) // append-string
        .serial(prePost()).serial(post()).serial(pre()) // chain
        .serial(prePost()).serial(prePost()).serial(post())

        //// async //This is unstable
        // .serial(prePost())
        // .serial(prePost())
        // .serial(prePost())

        // filter
        .serial(pre()).serial(prePost()).serial(post())

        // idempotent-message-filter
        .serial(pre()) // open message filter
        .serial(prePost()) // message processor
        .serial(post()) // close mf

        // idempotent-secure-hash-message-filter
        .serial(pre()) // open message filter
        .serial(prePost()) // message processor
        .serial(post()) // close mf

        // subflow
        .serial(prePost()).serial(pre()).serial(pre()).serial(prePost()).serial(post()).serial(post())

        // catch-es
        .serial(prePost()).serial(prePost())

        // rollback-es
        .serial(prePost()).serial(prePost())

        // choice-es
        .serial(prePost()).serial(prePost())

        // composite-source
        .serial(prePost()).serial(prePost())

        // first-successful
        .serial(prePost()) // logger
        .serial(pre()) // first-successful
        .serial(prePost()).serial(prePost()).serial(prePost()).serial(prePost()) // dlq
        .serial(post())

        // round-robin
        .serial(pre()) // round-robin
        .serial(prePost()) // inner logger
        .serial(post()).serial(prePost()) // logger

        // collection-aggregator
        .serial(pre()) // open Splitter, unpacks three messages
        .serial(prePost()) // 1st message on Logger
        .serial(prePost()) // gets to Aggregator
        .serial(prePost()) // 2nd message on Logger
        .serial(prePost()) // gets to Aggregator
        .serial(prePost()) // 3rd message on Logger
        .serial(prePost()) // gets to Aggregator and packs the three messages, then close
        .serial(post()) // close Splitter

        // custom-aggregator
        .serial(pre()) // open Splitter, unpacks three messages
        .serial(prePost()) // 1st message, open Aggregator
        .serial(prePost()) // 2nd message
        .serial(pre()) // 3rd message, packs the three messages
        .serial(prePost()) // Logger process packed message
        .serial(post()) // close Aggregator
        .serial(post()) // close Splitter

        // chunk-aggregator
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

        // wire-tap
        .serial(prePost()).serial(prePost())

        // until successful
        .serial(pre()).serial(new Node().parallel(prePost()).parallel(post().serial(prePost())))

        // until successful with processor chain
        .serial(pre())
        .serial(new Node().parallel(pre().serial(prePost()).serial(prePost()).serial(post())).parallel(post().serial(prePost())))

        // until successful with enricher
        .serial(pre()).serial(new Node().parallel(pre().serial(prePost()).serial(post())).parallel(post().serial(prePost())));
  }

  @Override
  public void validateSpecification(RestrictedNode spec) throws Exception {}

  public static class TestMessageSource implements MessageSource {

    private MessageProcessor listener;

    MuleEvent fireEvent(MuleEvent event) throws MuleException {
      return listener.process(event);
    }

    @Override
    public void setListener(MessageProcessor listener) {
      this.listener = listener;
    }

  }

}
