/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static org.mockito.Mockito.spy;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.tck.junit4.AbstractMuleTestCase.TEST_CONNECTOR;

import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleMessage.Builder;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.message.Correlation;
import org.mule.runtime.core.util.IOUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.activation.DataHandler;

import org.apache.commons.collections.Transformer;
import org.mockito.Mockito;

/**
 * Provides a fluent API for building events for testing.
 */
public class TestEventBuilder {

  private Object payload;
  private MediaType mediaType = MediaType.ANY;
  private Attributes attributes;
  private Map<String, Serializable> inboundProperties = new HashMap<>();
  private Map<String, Serializable> outboundProperties = new HashMap<>();
  private Map<String, DataHandler> inboundAttachments = new HashMap<>();
  private Map<String, Attachment> outboundAttachments = new HashMap<>();
  private Map<String, Object> sessionProperties = new HashMap<>();

  private String sourceCorrelationId = null;
  private Correlation correlation = null;

  private Map<String, Object> flowVariables = new HashMap<>();

  private MessageExchangePattern exchangePattern = REQUEST_RESPONSE;

  private boolean transacted = false;

  private ReplyToHandler replyToHandler;

  private Transformer spyTransformer = input -> input;

  /**
   * Prepares the given data to be sent as the payload of the product.
   *
   * @param payload the payload to use in the message
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder withPayload(Object payload) {
    this.payload = payload;

    return this;
  }


  /**
   * Prepares the given data to be sent as the mediaType of the payload of the {@link MuleEvent} to the configured flow.
   *
   * @param mediaType the mediaType to use in the message
   * @return this {@link FlowRunner}
   */
  public TestEventBuilder withMediaType(MediaType mediaType) {
    this.mediaType = mediaType;

    return this;
  }

  /**
   * Sets the {@link org.mule.runtime.api.message.MuleMessage#getAttributes()} value of the produced message
   *
   * @param attributes the attributes object for the produced {@link org.mule.runtime.api.message.MuleMessage}
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder withAttributes(Attributes attributes) {
    this.attributes = attributes;
    return this;
  }

  /**
   * Prepares a property with the given key and value to be sent as an inbound property of the product.
   *
   * @param key the key of the inbound property to add
   * @param value the value of the inbound property to add
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder withInboundProperty(String key, Serializable value) {
    inboundProperties.put(key, value);

    return this;
  }

  /**
   * Prepares the given properties map to be sent as inbound properties of the product.
   *
   * @param properties the inbound properties to add
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder withInboundProperties(Map<String, Serializable> properties) {
    inboundProperties.putAll(properties);

    return this;
  }

  /**
   * Prepares a property with the given key and value to be sent as an outbound property of the product.
   *
   * @param key the key of the outbound property to add
   * @param value the value of the outbound property to add
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder withOutboundProperty(String key, Serializable value) {
    outboundProperties.put(key, value);

    return this;
  }

  /**
   * Prepares an attachment with the given key and value to be sent in the product.
   *
   * @param key the key of the attachment to add
   * @param value the {@link DataHandler} for the attachment to add
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder withOutboundAttachment(String key, DataHandler value) {
    outboundAttachments.put(key, new DataHandlerAttachment(value));

    return this;
  }

  /**
   * Prepares an attachment with the given key and value to be sent in the product.
   *
   * @param key the key of the attachment to add
   * @param object the content of the attachment to add
   * @param contentType the content type of the attachment to add. Note that the charset attribute can be specifed too i.e.
   *        text/plain;charset=UTF-8
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder withOutboundAttachment(String key, Object object, MediaType contentType) {
    outboundAttachments.put(key, new ObjectAttachment(object, contentType));

    return this;
  }

  /**
   * Prepares an attachment with the given key and value to be sent in the product.
   *
   * @param key the key of the attachment to add
   * @param value the {@link DataHandler} for the attachment to add
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder withInboundAttachment(String key, DataHandler value) {
    inboundAttachments.put(key, value);

    return this;
  }

  /**
   * Prepares a property with the given key and value to be sent as a session property of the product.
   *
   * @param key the key of the session property to add
   * @param value the value of the session property to add
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder withSessionProperty(String key, Object value) {
    sessionProperties.put(key, value);

    return this;
  }

  /**
   * Configures the product event to have the provided {@code sourceCorrelationId}. See {@link MuleEvent#getCorrelationId()}.
   *
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder withSourceCorrelationId(String sourceCorrelationId) {
    this.sourceCorrelationId = sourceCorrelationId;

    return this;
  }

  /**
   * Configures the product event to have the provided {@code correlation}. See {@link MuleEvent#getCorrelation()}.
   *
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder withCorrelation(Correlation correlation) {
    this.correlation = correlation;

    return this;
  }

  /**
   * Prepares a flow variable with the given key and value to be set in the product.
   *
   * @param key the key of the flow variable to put
   * @param value the value of the flow variable to put
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder withFlowVariable(String key, Object value) {
    flowVariables.put(key, value);

    return this;
  }

  public TestEventBuilder transactionally() {
    transacted = true;

    return this;
  }

  /**
   * Configures the product event to run as one-way.
   *
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder asynchronously() {
    exchangePattern = ONE_WAY;

    return this;
  }

  /**
   * Configures the product event to have the provided {@link ReplyToHandler}.
   *
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder withReplyToHandler(ReplyToHandler replyToHandler) {
    this.replyToHandler = replyToHandler;

    return this;
  }

  /**
   * Configures the product event to have the provided {@link MessageExchangePattern}.
   *
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder withExchangePattern(MessageExchangePattern exchangePattern) {
    this.exchangePattern = exchangePattern;

    return this;
  }

  /**
   * Will spy the built {@link MuleMessage} and {@link MuleEvent}. See {@link Mockito#spy(Object) spy}.
   *
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder spyObjects() {
    spyTransformer = input -> spy(input);

    return this;
  }

  /**
   * Produces an event with the specified configuration.
   *
   * @param muleContext the context of the mule application
   * @param flow the recipient for the event to be built.
   * @return an event with the specified configuration.
   */
  public MuleEvent build(MuleContext muleContext, FlowConstruct flow) {
    final Builder messageBuilder;

    if (payload instanceof MuleMessage) {
      messageBuilder = MuleMessage.builder((MuleMessage) payload);
    } else {
      messageBuilder = MuleMessage.builder().payload(payload);
    }
    messageBuilder.mediaType(mediaType).inboundProperties(inboundProperties).outboundProperties(outboundProperties)
        .inboundAttachments(inboundAttachments);

    if (attributes != null) {
      messageBuilder.attributes(attributes);
    }
    final MuleMessage muleMessage = messageBuilder.build();

    MuleEvent event = MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR, sourceCorrelationId))
        .message((MuleMessage) spyTransformer.transform(muleMessage)).exchangePattern(exchangePattern).flow(flow)
        .replyToHandler(replyToHandler).transacted(transacted).build();

    for (Entry<String, Attachment> outboundAttachmentEntry : outboundAttachments.entrySet()) {
      outboundAttachmentEntry.getValue().addOutboundTo(event, outboundAttachmentEntry.getKey());
    }
    for (Entry<String, Object> sessionPropertyEntry : sessionProperties.entrySet()) {
      event.getSession().setProperty(sessionPropertyEntry.getKey(), sessionPropertyEntry.getValue());
    }

    for (Entry<String, Object> flowVarEntry : flowVariables.entrySet()) {
      event.setFlowVariable(flowVarEntry.getKey(), flowVarEntry.getValue());
    }

    return (MuleEvent) spyTransformer.transform(event);
  }

  private interface Attachment {

    void addOutboundTo(MuleEvent event, String key);
  }

  private class DataHandlerAttachment implements Attachment {

    private DataHandler dataHandler;

    public DataHandlerAttachment(DataHandler dataHandler) {
      this.dataHandler = dataHandler;
    }

    @Override
    public void addOutboundTo(MuleEvent event, String key) {
      event.setMessage(MuleMessage.builder(event.getMessage()).addOutboundAttachment(key, dataHandler).build());
    }
  }

  private class ObjectAttachment implements Attachment {

    private Object object;
    private MediaType contentType;

    public ObjectAttachment(Object object, MediaType contentType) {
      this.object = object;
      this.contentType = contentType;
    }

    @Override
    public void addOutboundTo(MuleEvent event, String key) {
      try {
        event.setMessage(MuleMessage.builder(event.getMessage())
            .addOutboundAttachment(key, IOUtils.toDataHandler(key, object, contentType)).build());
      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      }
    }
  }
}
