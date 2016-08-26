/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.mule.enricher;

import static java.nio.charset.StandardCharsets.UTF_16;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.metadata.MediaType.JSON;
import static org.mule.runtime.core.DefaultMuleEvent.getCurrentEvent;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.NonBlockingVoidMuleEvent;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.enricher.MessageEnricher;
import org.mule.runtime.core.enricher.MessageEnricher.EnrichExpressionPair;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChain;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.SensingNullReplyToHandler;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.matcher.DataTypeMatcher;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class MessageEnricherTestCase extends AbstractMuleContextTestCase {

  public static final String FOO_FLOW_VAR_EXPRESSION = "#[flowVars['foo']]";

  @Test
  public void testEnrichHeaderWithPayload() throws Exception {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.myHeader]"));
    enricher.setEnrichmentMessageProcessor(event -> {
      event.setMessage(MuleMessage.builder(event.getMessage()).payload("test").build());
      return event;
    });
    enricher.initialise();

    MuleMessage result = enricher.process(getTestEvent("")).getMessage();
    assertEquals("test", result.getOutboundProperty("myHeader"));
    assertEquals("", result.getPayload());
  }

  @Test
  public void testEnrichHeaderWithHeader() throws Exception {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.header1]",
                                                              "#[message.outboundProperties.myHeader]"));
    enricher.setEnrichmentMessageProcessor(event -> {
      event.setMessage(MuleMessage.builder(event.getMessage()).addOutboundProperty("header1", "test").build());
      return event;
    });

    MuleMessage result = enricher.process(getTestEvent("")).getMessage();
    assertEquals("test", result.getOutboundProperty("myHeader"));
    assertEquals("", result.getPayload());
  }

  @Test
  public void testEnrichHeadersMToN() throws Exception {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.header1]",
                                                              "#[message.outboundProperties.myHeader1]"));
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.header2]",
                                                              "#[message.outboundProperties.myHeader2]"));
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.header3]",
                                                              "#[message.outboundProperties.myHeader3]"));
    enricher.setEnrichmentMessageProcessor(event -> {
      event.setMessage(MuleMessage.builder(event.getMessage())
          .addOutboundProperty("header1", "test")
          .addOutboundProperty("header2", "test2")
          .addOutboundProperty("header3", "test3")
          .build());
      return event;
    });

    MuleMessage result = enricher.process(getTestEvent("")).getMessage();

    assertNull(result.getOutboundProperty("myHeader"));
    assertEquals("test2", result.getOutboundProperty("myHeader2"));
    assertEquals("test3", result.getOutboundProperty("myHeader3"));

    assertEquals("", result.getPayload());
  }

  @Test
  public void testEnrichWithNullResponse() throws Exception {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.myHeader]"));
    enricher.setEnrichmentMessageProcessor(event -> null);

    MuleMessage result = enricher.process(getTestEvent("")).getMessage();
    assertNull(result.getOutboundProperty("myHeader"));
    assertEquals("", result.getPayload());
  }

  @Test
  public void testEnrichWithException() throws Exception {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[header:myHeader]"));
    enricher.setEnrichmentMessageProcessor(event -> {
      throw new MessagingException(CoreMessages.createStaticMessage("Expected"), event);
    });

    try {
      enricher.process(getTestEvent(""));
      fail("Expected a MessagingException");
    } catch (MessagingException e) {
      assertThat(e.getMessage(), is("Expected."));
    }
    assertThat(getCurrentEvent().getReplyToHandler(), nullValue());
  }

  @Test
  public void propagateMessage() throws Exception {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.myHeader]"));
    enricher.setEnrichmentMessageProcessor(event -> {
      event.setMessage(MuleMessage.builder(event.getMessage()).payload("enriched").build());
      return event;
    });
    MuleEvent in = getTestEvent("");
    in.setMessage(MuleMessage.builder(in.getMessage()).addOutboundProperty("foo", "bar").build());
    MuleEvent out = enricher.process(in);
    assertThat(out, is(in));
    assertThat(out.getMessage(), is(in.getMessage()));
    assertThat(out.getCorrelationId(), equalTo(in.getCorrelationId()));
    assertThat(out.getMessage().getOutboundPropertyNames(), equalTo(in.getMessage().getOutboundPropertyNames()));
    assertThat(out.getMessage().getOutboundProperty("foo"), equalTo("bar"));
    assertThat(out.getMessage().getPayload(), equalTo(in.getMessage().getPayload()));
  }

  @Test
  public void propagatesVariables() throws Exception {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.myHeader]"));
    enricher.setEnrichmentMessageProcessor(event -> {
      event.setMessage(MuleMessage.builder(event.getMessage()).payload("enriched").build());
      return event;
    });
    MuleEvent in = getTestEvent("");
    in.getSession().setProperty("sessionFoo", "bar");
    in.setFlowVariable("flowFoo", "bar");

    MuleEvent out = enricher.process(in);

    assertEquals("bar", out.getSession().getProperty("sessionFoo"));
    assertEquals("bar", out.getFlowVariable("flowFoo"));
  }

  @Test
  public void doNotImplicitlyEnrichMessagePayload() throws Exception {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.myHeader]"));
    enricher.setEnrichmentMessageProcessor(event -> {
      event.setMessage(MuleMessage.builder(event.getMessage()).payload("enriched").build());
      return event;
    });
    MuleEvent in = getTestEvent("");

    MuleEvent out = enricher.process(in);

    assertEquals("", out.getMessage().getPayload());
  }

  @Test
  public void doNotImplicitlyEnrichMessageProperties() throws Exception {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.myHeader]"));
    enricher.setEnrichmentMessageProcessor(event -> {
      event.setMessage(MuleMessage.builder(event.getMessage()).addInboundProperty("foo", "bar").build());
      return event;
    });
    MuleEvent in = getTestEvent("");

    MuleEvent out = enricher.process(in);

    assertNull(out.getMessage().getOutboundProperty("foo"));
  }

  @Test
  public void doNotImplicitlyEnrichFlowVariable() throws Exception {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.myHeader]"));
    enricher.setEnrichmentMessageProcessor(event -> {
      event.setFlowVariable("flowFoo", "bar");
      return event;
    });
    MuleEvent in = getTestEvent("");

    MuleEvent out = enricher.process(in);

    assertThat(out.getFlowVariableNames(), not(contains("flowFoo")));
  }

  @Test
  public void doNotImplicitlyEnrichSessionVariable() throws Exception {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.myHeader]"));
    enricher.setEnrichmentMessageProcessor(event -> {
      event.getSession().setProperty("sessionFoo", "bar");
      return event;
    });
    MuleEvent in = getTestEvent("");

    MuleEvent out = enricher.process(in);

    assertNull(out.getSession().getProperty("sessionFoo"));
  }

  @Test
  public void enrichFlowVariable() throws Exception {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[flowVars.foo]"));
    enricher.setEnrichmentMessageProcessor(event -> {
      event.setMessage(MuleMessage.builder(event.getMessage()).payload("bar").build());
      return event;
    });
    MuleEvent in = getTestEvent("");

    MuleEvent out = enricher.process(in);

    assertEquals("bar", out.getFlowVariable("foo"));
  }

  @Test
  public void enrichSessionVariable() throws Exception {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[sessionVars['foo']]"));
    enricher.setEnrichmentMessageProcessor(event -> {
      event.setMessage(MuleMessage.builder(event.getMessage()).payload("bar").build());
      return event;
    });
    MuleEvent in = getTestEvent("");

    MuleEvent out = enricher.process(in);

    assertEquals("bar", out.getSession().getProperty("foo"));
  }

  @Test
  public void enrichesFlowVarWithDataType() throws Exception {
    doEnrichDataTypePropagationTest(new EnrichExpressionPair("#[payload]", FOO_FLOW_VAR_EXPRESSION));
  }

  @Test
  public void enrichesFlowVarWithDataTypeUsingExpressionEvaluator() throws Exception {
    doEnrichDataTypePropagationTest(new EnrichExpressionPair(FOO_FLOW_VAR_EXPRESSION));
  }

  @Test
  public void enricherConservesSameEventInstance() throws Exception {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[sessionVars['foo']]"));
    SensingNullMessageProcessor sensingNullMessageProcessor = new SensingNullMessageProcessor();
    enricher.setEnrichmentMessageProcessor(sensingNullMessageProcessor);

    Flow flow = mock(Flow.class);
    when(flow.getMuleContext()).thenReturn(muleContext);
    MuleEvent in = new DefaultMuleEvent(DefaultMessageContext.create(flow, TEST_CONNECTOR),
                                        MuleMessage.builder().payload(TEST_MESSAGE).build(),
                                        MessageExchangePattern.REQUEST_RESPONSE, flow);
    MuleEvent out = enricher.process(in);

    assertThat(out, is(sameInstance(in)));
    assertThat(sensingNullMessageProcessor.event, not(sameInstance(in)));
  }

  @Test
  public void enricherConservesSameEventInstanceNonBlockingTargetNonBlocking() throws Exception {
    SensingNullMessageProcessor sensingNullMessageProcessor = new SensingNullMessageProcessor();
    MessageEnricher enricher = createNonBlockingEnricher(sensingNullMessageProcessor);
    SensingNullReplyToHandler nullReplyToHandler = new SensingNullReplyToHandler();
    final MuleEvent in = createNonBlockingEvent(nullReplyToHandler);

    MuleEvent out = processEnricherInChain(enricher, in);

    nullReplyToHandler.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);

    assertThat(sensingNullMessageProcessor.event.getMessage(), sameInstance(in.getMessage()));

    assertThat(out, is(instanceOf(NonBlockingVoidMuleEvent.class)));
    assertThat(nullReplyToHandler.event.getMessage(), is(sameInstance(in.getMessage())));
  }

  @Test
  public void enricherConservesSameEventInstanceNonBlockingTargetBlocking() throws Exception {
    SensingNullMessageProcessor sensingNullMessageProcessor = new SensingNullMessageProcessor() {

      @Override
      public boolean isNonBlocking(MuleEvent event) {
        return false;
      }
    };
    MessageEnricher enricher = createNonBlockingEnricher(sensingNullMessageProcessor);

    SensingNullReplyToHandler nullReplyToHandler = new SensingNullReplyToHandler();
    final MuleEvent in = createNonBlockingEvent(nullReplyToHandler);

    MuleEvent out = processEnricherInChain(enricher, in);

    assertThat(sensingNullMessageProcessor.event.getMessage(), sameInstance(in.getMessage()));
    assertThat(out.getMessage(), is(sameInstance(in.getMessage())));
  }

  @Test
  public void testEnrichWithExceptionNonBlocking() throws Exception {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[header:myHeader]"));
    enricher.setEnrichmentMessageProcessor(event -> {
      throw new MessagingException(CoreMessages.createStaticMessage("Expected"), event);
    });

    try {
      SensingNullReplyToHandler nullReplyToHandler = new SensingNullReplyToHandler();
      enricher.process(createNonBlockingEvent(nullReplyToHandler));
      fail("Expected a MessagingException");
    } catch (MessagingException e) {
      assertThat(e.getMessage(), is("Expected."));
    }
    assertThat(getCurrentEvent().getReplyToHandler(), instanceOf(ReplyToHandler.class));
  }

  private MuleEvent createNonBlockingEvent(SensingNullReplyToHandler nullReplyToHandler) {
    Flow flow = mock(Flow.class);
    when(flow.getProcessingStrategy()).thenReturn(new NonBlockingProcessingStrategy());
    when(flow.getMuleContext()).thenReturn(muleContext);

    return MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR)).message(MuleMessage.builder().payload(TEST_MESSAGE).build())
        .exchangePattern(REQUEST_RESPONSE).replyToHandler(nullReplyToHandler).flow(flow).build();
  }

  private MessageEnricher createNonBlockingEnricher(SensingNullMessageProcessor sensingNullMessageProcessor) {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[sessionVars['foo']]"));
    enricher.setEnrichmentMessageProcessor(sensingNullMessageProcessor);
    return enricher;
  }

  private MuleEvent processEnricherInChain(MessageEnricher enricher, final MuleEvent in) throws MuleException {
    return DefaultMessageProcessorChain.from(muleContext, enricher, event -> {
      assertThat(event.getMessage(), is(sameInstance(in.getMessage())));
      return event;
    }).process(in);
  }

  private void doEnrichDataTypePropagationTest(EnrichExpressionPair pair) throws Exception {
    final DataType dataType = DataType.builder().type(String.class).mediaType(JSON).charset(UTF_16.name()).build();

    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);

    enricher.addEnrichExpressionPair(pair);
    enricher.setEnrichmentMessageProcessor(event -> {
      event.setMessage(MuleMessage.builder(event.getMessage()).payload("bar").mediaType(dataType.getMediaType()).build());
      return event;
    });
    MuleEvent in = getTestEvent("");

    MuleEvent out = enricher.process(in);

    assertEquals("bar", out.getFlowVariable("foo"));
    assertThat(out.getFlowVariableDataType("foo"), DataTypeMatcher.like(String.class, JSON, UTF_16));
  }
}
