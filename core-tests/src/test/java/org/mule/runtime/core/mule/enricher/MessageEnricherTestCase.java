/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.mule.enricher;

import static java.nio.charset.StandardCharsets.UTF_16;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.contains;
import static org.mule.runtime.api.metadata.MediaType.JSON;
import static org.mule.runtime.core.api.Event.getCurrentEvent;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.enricher.MessageEnricher;
import org.mule.runtime.core.enricher.MessageEnricher.EnrichExpressionPair;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.matcher.DataTypeMatcher;

import org.junit.Test;

public class MessageEnricherTestCase extends AbstractMuleContextTestCase {

  public static final String FOO_FLOW_VAR_EXPRESSION = "#[flowVars['foo']]";

  @Test
  public void testEnrichHeaderWithPayload() throws Exception {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.myHeader]"));
    enricher.setEnrichmentMessageProcessor(event -> {
      return Event.builder(event).message(InternalMessage.builder(event.getMessage()).payload("test").build()).build();
    });
    enricher.initialise();

    InternalMessage result = enricher.process(testEvent()).getMessage();
    assertEquals("test", result.getOutboundProperty("myHeader"));
    assertEquals(TEST_PAYLOAD, result.getPayload().getValue());
  }

  @Test
  public void testEnrichHeaderWithHeader() throws Exception {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.header1]",
                                                              "#[message.outboundProperties.myHeader]"));
    enricher.setEnrichmentMessageProcessor(event -> {
      return Event.builder(event)
          .message(InternalMessage.builder(event.getMessage()).addOutboundProperty("header1", "test").build()).build();
    });

    InternalMessage result = enricher.process(testEvent()).getMessage();
    assertEquals("test", result.getOutboundProperty("myHeader"));
    assertEquals(TEST_PAYLOAD, result.getPayload().getValue());
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
      return Event.builder(event).message(InternalMessage.builder(event.getMessage())
          .addOutboundProperty("header1", "test")
          .addOutboundProperty("header2", "test2")
          .addOutboundProperty("header3", "test3")
          .build()).build();
    });

    InternalMessage result = enricher.process(testEvent()).getMessage();

    assertNull(result.getOutboundProperty("myHeader"));
    assertEquals("test2", result.getOutboundProperty("myHeader2"));
    assertEquals("test3", result.getOutboundProperty("myHeader3"));

    assertEquals(TEST_PAYLOAD, result.getPayload().getValue());
  }

  @Test
  public void testEnrichWithNullResponse() throws Exception {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.myHeader]"));
    enricher.setEnrichmentMessageProcessor(event -> null);

    InternalMessage result = enricher.process(testEvent()).getMessage();
    assertNull(result.getOutboundProperty("myHeader"));
    assertEquals(TEST_PAYLOAD, result.getPayload().getValue());
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
      enricher.process(testEvent());
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
      return Event.builder(event).message(InternalMessage.builder(event.getMessage()).payload("enriched").build()).build();
    });
    Event in = eventBuilder().message(InternalMessage.builder().payload("").addOutboundProperty("foo", "bar").build()).build();
    Event out = enricher.process(in);
    assertThat(out.getCorrelationId(), equalTo(in.getCorrelationId()));
    assertThat(out.getMessage().getOutboundProperty("foo"), equalTo("bar"));
    assertThat(out.getMessage().getPayload().getValue(), equalTo(in.getMessage().getPayload().getValue()));
  }

  @Test
  public void propagatesVariables() throws Exception {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.myHeader]"));
    enricher.setEnrichmentMessageProcessor(event -> {
      return Event.builder(event).message(InternalMessage.builder(event.getMessage()).payload("enriched").build()).build();
    });
    Event in = eventBuilder().message(InternalMessage.of("")).addVariable("flowFoo", "bar").build();
    in.getSession().setProperty("sessionFoo", "bar");

    Event out = enricher.process(in);

    assertEquals("bar", out.getSession().getProperty("sessionFoo"));
    assertEquals("bar", out.getVariable("flowFoo").getValue());
  }

  @Test
  public void doNotImplicitlyEnrichMessagePayload() throws Exception {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.myHeader]"));
    enricher.setEnrichmentMessageProcessor(event -> {
      return Event.builder(event).message(InternalMessage.builder(event.getMessage()).payload("enriched").build()).build();
    });
    Event out = enricher.process(testEvent());

    assertEquals(TEST_PAYLOAD, out.getMessage().getPayload().getValue());
  }

  @Test
  public void doNotImplicitlyEnrichMessageProperties() throws Exception {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.myHeader]"));
    enricher.setEnrichmentMessageProcessor(event -> {
      return Event.builder(event).message(InternalMessage.builder(event.getMessage()).addInboundProperty("foo", "bar").build())
          .build();
    });
    Event out = enricher.process(testEvent());

    assertNull(out.getMessage().getOutboundProperty("foo"));
  }

  @Test
  public void doNotImplicitlyEnrichFlowVariable() throws Exception {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.myHeader]"));
    enricher.setEnrichmentMessageProcessor(event -> {
      return Event.builder(event).addVariable("flowFoo", "bar").build();
    });
    Event out = enricher.process(testEvent());

    assertThat(out.getVariableNames(), not(contains("flowFoo")));
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
    Event out = enricher.process(testEvent());

    assertNull(out.getSession().getProperty("sessionFoo"));
  }

  @Test
  public void enrichFlowVariable() throws Exception {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[flowVars.foo]"));
    enricher.setEnrichmentMessageProcessor(event -> {
      return Event.builder(event).message(InternalMessage.builder(event.getMessage()).payload("bar").build()).build();
    });
    Event out = enricher.process(testEvent());

    assertEquals("bar", out.getVariable("foo").getValue());
  }

  @Test
  public void enrichSessionVariable() throws Exception {
    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);
    enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[sessionVars['foo']]"));
    enricher.setEnrichmentMessageProcessor(event -> {
      return Event.builder(event).message(InternalMessage.builder(event.getMessage()).payload("bar").build()).build();
    });
    Event out = enricher.process(testEvent());

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

  private void doEnrichDataTypePropagationTest(EnrichExpressionPair pair) throws Exception {
    final DataType dataType = DataType.builder().type(String.class).mediaType(JSON).charset(UTF_16.name()).build();

    MessageEnricher enricher = new MessageEnricher();
    enricher.setMuleContext(muleContext);

    enricher.addEnrichExpressionPair(pair);
    enricher.setEnrichmentMessageProcessor(event -> {
      return Event.builder(event)
          .message(InternalMessage.builder(event.getMessage()).payload("bar").mediaType(dataType.getMediaType()).build()).build();
    });
    Event out = enricher.process(testEvent());

    assertEquals("bar", out.getVariable("foo").getValue());
    assertThat(out.getVariable("foo").getDataType(), DataTypeMatcher.like(String.class, JSON, UTF_16));
  }
}
