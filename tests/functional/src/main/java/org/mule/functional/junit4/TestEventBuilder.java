/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static org.mockito.Mockito.spy;
import static org.mule.tck.junit4.AbstractMuleTestCase.TEST_CONNECTOR;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.message.InternalMessage.Builder;
import org.mule.runtime.core.message.DefaultMultiPartPayload;
import org.mule.runtime.core.message.GroupCorrelation;
import org.mule.runtime.core.util.IOUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.activation.DataHandler;

import org.apache.commons.collections.Transformer;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;

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
  private GroupCorrelation correlation = new GroupCorrelation(null, null);

  private Map<String, Object> variables = new HashMap<>();

  private ReplyToHandler replyToHandler;

  private Transformer spyTransformer = input -> input;

  private Publisher<Void> externalCompletionCallback = null;

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
   * Prepares the given data to be sent as the mediaType of the payload of the {@link Event} to the configured flow.
   *
   * @param mediaType the mediaType to use in the message
   * @return this {@link FlowRunner}
   */
  public TestEventBuilder withMediaType(MediaType mediaType) {
    this.mediaType = mediaType;

    return this;
  }

  /**
   * Sets the {@link org.mule.runtime.api.message.Message#getAttributes()} value of the produced message
   *
   * @param attributes the attributes object for the produced {@link org.mule.runtime.api.message.Message}
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder withAttributes(Attributes attributes) {
    this.attributes = attributes;
    return this;
  }

  /**
   * Prepares a property with the given key and value to be sent as an inbound property of the product.
   *
   * @param key   the key of the inbound property to add
   * @param value the value of the inbound property to add
   * @return this {@link TestEventBuilder}
   * @deprecated Transport infrastructure is deprecated. Use {@link Attributes} instead.
   */
  @Deprecated
  public TestEventBuilder withInboundProperty(String key, Serializable value) {
    inboundProperties.put(key, value);

    return this;
  }

  /**
   * Prepares the given properties map to be sent as inbound properties of the product.
   *
   * @param properties the inbound properties to add
   * @return this {@link TestEventBuilder}
   * @deprecated Transport infrastructure is deprecated. Use {@link Attributes} instead.
   */
  @Deprecated
  public TestEventBuilder withInboundProperties(Map<String, Serializable> properties) {
    inboundProperties.putAll(properties);

    return this;
  }

  /**
   * Prepares a property with the given key and value to be sent as an outbound property of the product.
   *
   * @param key   the key of the outbound property to add
   * @param value the value of the outbound property to add
   * @return this {@link TestEventBuilder}
   * @deprecated Transport infrastructure is deprecated. Use {@link Attributes} instead.
   */
  @Deprecated
  public TestEventBuilder withOutboundProperty(String key, Serializable value) {
    outboundProperties.put(key, value);

    return this;
  }

  /**
   * Prepares an attachment with the given key and value to be sent in the product.
   *
   * @param key   the key of the attachment to add
   * @param value the {@link DataHandler} for the attachment to add
   * @return this {@link TestEventBuilder}
   * @deprecated Transport infrastructure is deprecated. Use {@link DefaultMultiPartPayload} instead.
   */
  @Deprecated
  public TestEventBuilder withOutboundAttachment(String key, DataHandler value) {
    outboundAttachments.put(key, new DataHandlerAttachment(value));

    return this;
  }

  /**
   * Prepares an attachment with the given key and value to be sent in the product.
   *
   * @param key         the key of the attachment to add
   * @param object      the content of the attachment to add
   * @param contentType the content type of the attachment to add. Note that the charset attribute can be specifed too i.e.
   *                    text/plain;charset=UTF-8
   * @return this {@link TestEventBuilder}
   * @deprecated Transport infrastructure is deprecated. Use {@link DefaultMultiPartPayload} instead.
   */
  @Deprecated
  public TestEventBuilder withOutboundAttachment(String key, Object object, MediaType contentType) {
    outboundAttachments.put(key, new ObjectAttachment(object, contentType));

    return this;
  }

  /**
   * Prepares an attachment with the given key and value to be sent in the product.
   *
   * @param key   the key of the attachment to add
   * @param value the {@link DataHandler} for the attachment to add
   * @return this {@link TestEventBuilder}
   * @deprecated Transport infrastructure is deprecated. Use {@link DefaultMultiPartPayload} instead.
   */
  @Deprecated
  public TestEventBuilder withInboundAttachment(String key, DataHandler value) {
    inboundAttachments.put(key, value);

    return this;
  }

  /**
   * Prepares a property with the given key and value to be sent as a session property of the product.
   *
   * @param key   the key of the session property to add
   * @param value the value of the session property to add
   * @return this {@link TestEventBuilder}
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
  public TestEventBuilder withSessionProperty(String key, Object value) {
    sessionProperties.put(key, value);

    return this;
  }

  /**
   * Configures the product event to have the provided {@code sourceCorrelationId}. See {@link Event#getCorrelationId()}.
   *
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder withSourceCorrelationId(String sourceCorrelationId) {
    this.sourceCorrelationId = sourceCorrelationId;

    return this;
  }

  /**
   * Configures the product event to have the provided {@code correlation}. See {@link Event#getGroupCorrelation()}.
   *
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder withCorrelation(GroupCorrelation correlation) {
    this.correlation = correlation;

    return this;
  }

  /**
   * Prepares a flow variable with the given key and value to be set in the product.
   *
   * @param key   the key of the flow variable to put
   * @param value the value of the flow variable to put
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder withVariable(String key, Object value) {
    variables.put(key, value);

    return this;
  }

  /**
   * Configures the product event to have the provided {@link ReplyToHandler}.
   *
   * @return this {@link TestEventBuilder}
   * @deprecated TODO MULE-10739 Move ReplyToHandler to compatibility module.
   */
  @Deprecated
  public TestEventBuilder withReplyToHandler(ReplyToHandler replyToHandler) {
    this.replyToHandler = replyToHandler;

    return this;
  }

  /**
   * Will spy the built {@link Message} and {@link Event}. See {@link Mockito#spy(Object) spy}.
   *
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder spyObjects() {
    spyTransformer = input -> spy(input);

    return this;
  }

  public TestEventBuilder setExternalCompletionCallback(Publisher<Void> externalCompletionCallback) {
    this.externalCompletionCallback = externalCompletionCallback;
    return this;
  }

  /**
   * Produces an event with the specified configuration.
   *
   * @param muleContext the context of the mule application
   * @param flow        the recipient for the event to be built.
   * @return an event with the specified configuration.
   */
  public Event build(MuleContext muleContext, FlowConstruct flow) {
    final Builder messageBuilder;

    messageBuilder = InternalMessage.builder().payload(payload).mediaType(mediaType).inboundProperties(inboundProperties)
        .outboundProperties(outboundProperties).inboundAttachments(inboundAttachments);

    if (attributes != null) {
      messageBuilder.attributes(attributes);
    }
    final Message muleMessage = messageBuilder.build();

    EventContext eventContext;
    if (externalCompletionCallback != null) {
      eventContext = DefaultEventContext.create(flow, TEST_CONNECTOR, sourceCorrelationId, externalCompletionCallback);
    } else {
      eventContext = DefaultEventContext.create(flow, TEST_CONNECTOR, sourceCorrelationId);
    }

    Event event = Event.builder(eventContext)
        .message((Message) spyTransformer.transform(muleMessage)).variables(variables).groupCorrelation(correlation)
        .flow(flow).replyToHandler(replyToHandler).build();

    for (Entry<String, Attachment> outboundAttachmentEntry : outboundAttachments.entrySet()) {
      event = outboundAttachmentEntry.getValue().addOutboundTo(event, outboundAttachmentEntry.getKey());
    }
    for (Entry<String, Object> sessionPropertyEntry : sessionProperties.entrySet()) {
      event.getSession().setProperty(sessionPropertyEntry.getKey(), sessionPropertyEntry.getValue());
    }

    return (Event) spyTransformer.transform(event);
  }

  private interface Attachment {

    Event addOutboundTo(Event event, String key);
  }


  private class DataHandlerAttachment implements Attachment {

    private DataHandler dataHandler;

    public DataHandlerAttachment(DataHandler dataHandler) {
      this.dataHandler = dataHandler;
    }

    @Override
    public Event addOutboundTo(Event event, String key) {
      return Event.builder(event)
          .message(InternalMessage.builder(event.getMessage()).addOutboundAttachment(key, dataHandler).build()).build();
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
    public Event addOutboundTo(Event event, String key) {
      try {
        return Event.builder(event).message(InternalMessage.builder(event.getMessage())
            .addOutboundAttachment(key, IOUtils.toDataHandler(key, object, contentType))
            .build()).build();
      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      }
    }
  }
}
