/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.enricher;

import static java.nio.charset.StandardCharsets.UTF_16;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.MediaType.JSON;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.enricher.MessageEnricher.EnrichExpressionPair;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.processor.InternalProcessor;
import org.mule.tck.junit4.AbstractReactiveProcessorTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MessageEnricherTestCase extends AbstractReactiveProcessorTestCase {

  public static final String FOO_FLOW_VAR_EXPRESSION = "#[mel:flowVars['foo']]";

  @Rule
  public ExpectedException thrown = none();

  public MessageEnricherTestCase(Mode mode) {
    super(mode);
  }

  @Test
  public void testEnrichHeaderWithPayload() throws Exception {
    MessageEnricher enricher = createEnricher();
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[mel:message.outboundProperties.myHeader]"));
    enricher.setEnrichmentMessageProcessor((InternalTestProcessor) event -> CoreEvent.builder(event)
        .message(InternalMessage.builder(event.getMessage()).value(TEST_PAYLOAD).build()).build());
    initialiseIfNeeded(enricher, muleContext);

    Message result = process(enricher, testEvent()).getMessage();
    assertEquals(TEST_PAYLOAD, ((InternalMessage) result).getOutboundProperty("myHeader"));
    assertEquals(TEST_PAYLOAD, result.getPayload().getValue());
  }

  private MessageEnricher createEnricher() {
    MessageEnricher enricher = baseEnricher();
    enricher.setAnnotations(getAppleFlowComponentLocationAnnotations());
    return enricher;
  }

  @Test
  public void testEnrichHeaderWithHeader() throws Exception {
    MessageEnricher enricher = createEnricher();
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[mel:message.outboundProperties.header1]",
                                                              "#[mel:message.outboundProperties.myHeader]"));
    enricher.setEnrichmentMessageProcessor((InternalTestProcessor) event -> CoreEvent.builder(event)
        .message(InternalMessage.builder(event.getMessage()).addOutboundProperty("header1", "test").build()).build());

    Message result = process(enricher, testEvent()).getMessage();
    assertEquals("test", ((InternalMessage) result).getOutboundProperty("myHeader"));
    assertEquals(TEST_PAYLOAD, result.getPayload().getValue());
  }

  @Test
  public void testEnrichHeadersMToN() throws Exception {
    MessageEnricher enricher = baseEnricher();
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[mel:message.outboundProperties.header1]",
                                                              "#[mel:message.outboundProperties.myHeader1]"));
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[mel:message.outboundProperties.header2]",
                                                              "#[mel:message.outboundProperties.myHeader2]"));
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[mel:message.outboundProperties.header3]",
                                                              "#[mel:message.outboundProperties.myHeader3]"));
    enricher
        .setEnrichmentMessageProcessor((InternalTestProcessor) event -> CoreEvent.builder(event)
            .message(InternalMessage.builder(event.getMessage())
                .addOutboundProperty("header1", "test")
                .addOutboundProperty("header2", "test2")
                .addOutboundProperty("header3", "test3")
                .build())
            .build());

    Message result = process(enricher, testEvent()).getMessage();

    assertNull(((InternalMessage) result).getOutboundProperty("myHeader"));
    assertEquals("test2", ((InternalMessage) result).getOutboundProperty("myHeader2"));
    assertEquals("test3", ((InternalMessage) result).getOutboundProperty("myHeader3"));
    assertEquals(TEST_PAYLOAD, result.getPayload().getValue());
  }

  @Test
  public void testEnrichWithNullResponse() throws Exception {
    MessageEnricher enricher = baseEnricher();
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[mel:message.outboundProperties.myHeader]"));
    enricher.setEnrichmentMessageProcessor((InternalTestProcessor) event -> null);

    Message result = process(enricher, testEvent()).getMessage();
    assertNull(((InternalMessage) result).getOutboundProperty("myHeader"));
    assertEquals(TEST_PAYLOAD, result.getPayload().getValue());
  }

  @Test
  public void testEnrichWithException() throws Exception {
    IllegalStateException testException = new IllegalStateException();
    MessageEnricher enricher = baseEnricher();
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[mel:header:myHeader]"));
    enricher.setEnrichmentMessageProcessor((InternalTestProcessor) event -> {
      throw testException;
    });

    thrown.expect(sameInstance(testException));
    process(enricher, testEvent());
  }

  @Test
  public void propagateMessage() throws Exception {
    MessageEnricher enricher = baseEnricher();
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[mel:message.outboundProperties.myHeader]"));
    enricher.setEnrichmentMessageProcessor((InternalTestProcessor) event -> CoreEvent.builder(event)
        .message(InternalMessage.builder(event.getMessage()).value("enriched").build()).build());
    CoreEvent in =
        eventBuilder(muleContext).message(InternalMessage.builder().value("").addOutboundProperty("foo", "bar").build()).build();
    CoreEvent out = process(enricher, in);
    assertThat(out.getCorrelationId(), equalTo(in.getCorrelationId()));
    assertThat(((InternalMessage) out.getMessage()).getOutboundProperty("foo"), equalTo("bar"));
    assertThat(out.getMessage().getPayload().getValue(), equalTo(in.getMessage().getPayload().getValue()));
  }

  @Test
  public void propagatesVariables() throws Exception {
    MessageEnricher enricher = baseEnricher();
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[mel:message.outboundProperties.myHeader]"));
    enricher.setEnrichmentMessageProcessor((InternalTestProcessor) event -> CoreEvent.builder(event)
        .message(InternalMessage.builder(event.getMessage()).value("enriched").build()).build());
    CoreEvent in = eventBuilder(muleContext).message(of("")).addVariable("flowFoo", "bar").build();
    ((PrivilegedEvent) in).getSession().setProperty("sessionFoo", "bar");

    CoreEvent out = process(enricher, in);

    assertEquals("bar", ((PrivilegedEvent) out).getSession().getProperty("sessionFoo"));
    assertEquals("bar", out.getVariables().get("flowFoo").getValue());
  }

  @Test
  public void doNotImplicitlyEnrichMessagePayload() throws Exception {
    MessageEnricher enricher = baseEnricher();
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[mel:message.outboundProperties.myHeader]"));
    enricher.setEnrichmentMessageProcessor((InternalTestProcessor) event -> CoreEvent.builder(event)
        .message(InternalMessage.builder(event.getMessage()).value("enriched").build()).build());
    Message out = process(enricher, testEvent()).getMessage();
    assertEquals(TEST_PAYLOAD, out.getPayload().getValue());
  }

  @Test
  public void doNotImplicitlyEnrichMessageProperties() throws Exception {
    MessageEnricher enricher = baseEnricher();
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[mel:message.outboundProperties.myHeader]"));
    enricher.setEnrichmentMessageProcessor((InternalTestProcessor) event -> CoreEvent.builder(event)
        .message(InternalMessage.builder(event.getMessage()).addInboundProperty("foo", "bar").build())
        .build());

    Message out = process(enricher, testEvent()).getMessage();
    assertNull(((InternalMessage) out).getOutboundProperty("foo"));
  }

  @Test
  public void doNotImplicitlyEnrichFlowVariable() throws Exception {
    MessageEnricher enricher = baseEnricher();
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[mel:message.outboundProperties.myHeader]"));
    enricher.setEnrichmentMessageProcessor((InternalTestProcessor) event -> CoreEvent.builder(event)
        .addVariable("flowFoo", "bar").build());
    CoreEvent out = process(enricher, testEvent());
    assertThat(out.getVariables().keySet(), not(hasItem("flowFoo")));
  }

  @Test
  public void doNotImplicitlyEnrichSessionVariable() throws Exception {
    MessageEnricher enricher = baseEnricher();
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[mel:message.outboundProperties.myHeader]"));
    enricher.setEnrichmentMessageProcessor((InternalTestProcessor) event -> {
      ((PrivilegedEvent) event).getSession().setProperty("sessionFoo", "bar");
      return event;
    });
    CoreEvent out = process(enricher, testEvent());
    assertNull(((PrivilegedEvent) out).getSession().getProperty("sessionFoo"));
  }

  @Test
  public void enrichFlowVariable() throws Exception {
    MessageEnricher enricher = baseEnricher();
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[mel:flowVars.foo]"));
    enricher.setEnrichmentMessageProcessor((InternalTestProcessor) event -> CoreEvent.builder(event)
        .message(InternalMessage.builder(event.getMessage()).value("bar").build()).build());
    CoreEvent out = process(enricher, testEvent());
    assertEquals("bar", out.getVariables().get("foo").getValue());
  }

  @Test
  public void enrichSessionVariable() throws Exception {
    MessageEnricher enricher = baseEnricher();
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[mel:sessionVars['foo']]"));
    enricher.setEnrichmentMessageProcessor((InternalTestProcessor) event -> CoreEvent.builder(event)
        .message(InternalMessage.builder(event.getMessage()).value("bar").build()).build());
    CoreEvent out = process(enricher, testEvent());
    assertEquals("bar", ((PrivilegedEvent) out).getSession().getProperty("foo"));
  }

  @Test
  public void enrichesFlowVarWithDataType() throws Exception {
    doEnrichDataTypePropagationTest(new EnrichExpressionPair("#[mel:payload]", FOO_FLOW_VAR_EXPRESSION));
  }

  @Test
  public void enrichesFlowVarWithDataTypeUsingExpressionEvaluator() throws Exception {
    doEnrichDataTypePropagationTest(new EnrichExpressionPair(FOO_FLOW_VAR_EXPRESSION));
  }

  private void doEnrichDataTypePropagationTest(EnrichExpressionPair pair) throws Exception {
    final DataType dataType = DataType.builder().type(String.class).mediaType(JSON).charset(UTF_16.name()).build();

    MessageEnricher enricher = baseEnricher();

    enricher.addEnrichExpressionPair(pair);
    enricher.setEnrichmentMessageProcessor((InternalTestProcessor) event -> CoreEvent.builder(event)
        .message(InternalMessage.builder(event.getMessage()).value("bar").mediaType(dataType.getMediaType()).build()).build());
    CoreEvent out = process(enricher, testEvent());
    assertEquals("bar", out.getVariables().get("foo").getValue());
    assertThat(out.getVariables().get("foo").getDataType(), like(String.class, JSON, UTF_16));
  }

  public MessageEnricher baseEnricher() {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setAnnotations(getAppleFlowComponentLocationAnnotations());
    enricher.setMuleContext(muleContext);
    return enricher;
  }

  @FunctionalInterface
  private interface InternalTestProcessor extends Processor, InternalProcessor {

  }

}
