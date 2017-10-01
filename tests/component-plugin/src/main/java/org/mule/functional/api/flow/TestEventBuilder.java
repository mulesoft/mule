/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.flow;

import static java.util.Optional.ofNullable;
import static org.mockito.Mockito.spy;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.tck.junit4.AbstractMuleTestCase.TEST_CONNECTOR_LOCATION;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.privileged.connector.ReplyToHandler;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.message.GroupCorrelation;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import org.mockito.Mockito;
import org.reactivestreams.Publisher;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import javax.activation.DataHandler;

/**
 * Provides a fluent API for building events for testing.
 */
public class TestEventBuilder {

  private Object payload;
  private Object attributes;
  private MediaType mediaType = MediaType.ANY;
  private Map<String, Serializable> inboundProperties = new HashMap<>();
  private Map<String, Serializable> outboundProperties = new HashMap<>();
  private Map<String, DataHandler> inboundAttachments = new HashMap<>();
  private Map<String, Attachment> outboundAttachments = new HashMap<>();
  private Map<String, Object> sessionProperties = new HashMap<>();

  private String sourceCorrelationId = null;
  private GroupCorrelation groupCorrelation;

  private Map<String, TypedValue> variables = new HashMap<>();

  private ReplyToHandler replyToHandler;

  private Function<Message, Message> spyMessage = input -> input;
  private Function<CoreEvent, CoreEvent> spyEvent = input -> input;

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
   * Prepares the given data to be sent as the mediaType of the payload of the {@link CoreEvent} to the configured flow.
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
  public TestEventBuilder withAttributes(Object attributes) {
    this.attributes = attributes;
    return this;
  }

  /**
   * Prepares a property with the given key and value to be sent as an inbound property of the product.
   *
   * @param key the key of the inbound property to add
   * @param value the value of the inbound property to add
   * @return this {@link TestEventBuilder}
   * @deprecated Transport infrastructure is deprecated. Use {@link Message#getAttributes()} instead.
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
   * @deprecated Transport infrastructure is deprecated. Use {@link Message#getAttributes()} instead.
   */
  @Deprecated
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
   * @deprecated Transport infrastructure is deprecated. Use {@link Message#getAttributes()} instead.
   */
  @Deprecated
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
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
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
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
  public TestEventBuilder withSessionProperty(String key, Object value) {
    sessionProperties.put(key, value);

    return this;
  }

  /**
   * Configures the product event to have the provided {@code sourceCorrelationId}. See {@link CoreEvent#getCorrelationId()}.
   *
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder withSourceCorrelationId(String sourceCorrelationId) {
    this.sourceCorrelationId = sourceCorrelationId;

    return this;
  }

  /**
   * Configures the product event to have the provided {@code correlation}. See {@link CoreEvent#getGroupCorrelation()}.
   *
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder withCorrelation(GroupCorrelation groupCorrelation) {
    this.groupCorrelation = groupCorrelation;

    return this;
  }

  /**
   * Prepares a flow variable with the given key and value to be set in the product.
   *
   * @param key the key of the flow variable to put
   * @param value the value of the flow variable to put
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder withVariable(String key, Object value) {
    variables.put(key, new TypedValue(value, null));

    return this;
  }

  /**
   * Prepares a flow variable with the given key and value to be set in the product.
   *
   * @param key the key of the flow variable to put
   * @param value the value of the flow variable to put
   * @param dataType the data type of the variable
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder withVariable(String key, Object value, DataType dataType) {
    variables.put(key, new TypedValue(value, dataType));

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
   * Will spy the built {@link Message} and {@link CoreEvent}. See {@link Mockito#spy(Object) spy}.
   *
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder spyObjects() {
    spyMessage = input -> spy(input);
    spyEvent = input -> spy(input);

    return this;
  }

  public TestEventBuilder setExternalCompletionCallback(Publisher<Void> externalCompletionCallback) {
    this.externalCompletionCallback = externalCompletionCallback;
    return this;
  }

  /**
   * Produces an event with the specified configuration.
   *
   * @param flow the recipient for the event to be built.
   * @return an event with the specified configuration.
   */
  public CoreEvent build(FlowConstruct flow) {
    final Message.Builder messageBuilder;

    messageBuilder = Message.builder().value(payload).mediaType(mediaType);

    setInboundProperties(messageBuilder, inboundProperties);
    setOutboundProperties(messageBuilder, outboundProperties);

    if (attributes != null) {
      messageBuilder.attributesValue(attributes);
    }
    final Message muleMessage = messageBuilder.build();

    EventContext eventContext;
    if (externalCompletionCallback != null) {
      eventContext = create(flow, TEST_CONNECTOR_LOCATION, sourceCorrelationId, externalCompletionCallback);
    } else {
      eventContext = create(flow, TEST_CONNECTOR_LOCATION, sourceCorrelationId);
    }

    CoreEvent.Builder builder = InternalEvent.builder(eventContext)
        .message(spyMessage.apply(muleMessage)).groupCorrelation(ofNullable(groupCorrelation))
        .replyToHandler(replyToHandler);
    for (Entry<String, TypedValue> variableEntry : variables.entrySet()) {
      builder.addVariable(variableEntry.getKey(), variableEntry.getValue().getValue(), variableEntry.getValue().getDataType());
    }
    CoreEvent event = builder.build();

    for (Entry<String, Attachment> outboundAttachmentEntry : outboundAttachments.entrySet()) {
      event = outboundAttachmentEntry.getValue().addOutboundTo(event, outboundAttachmentEntry.getKey());
    }
    for (Entry<String, Object> sessionPropertyEntry : sessionProperties.entrySet()) {
      ((PrivilegedEvent) event).getSession().setProperty(sessionPropertyEntry.getKey(), sessionPropertyEntry.getValue());
    }

    return spyEvent.apply(event);
  }

  private void setInboundProperties(Message.Builder messageBuilder, Map<String, Serializable> inboundProperties) {
    // TODO(pablo.kraan): MULE-12280 - remove methods that use the legacy message API once all the tests using it are migrated
    try {
      Method inboundPropertiesMethod = messageBuilder.getClass().getMethod("inboundProperties", Map.class);
      inboundPropertiesMethod.invoke(messageBuilder, inboundProperties);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private void setOutboundProperties(Message.Builder messageBuilder, Map<String, Serializable> outboundProperties) {
    try {
      Method outboundPropertiesMethod = messageBuilder.getClass().getMethod("outboundProperties", Map.class);
      outboundPropertiesMethod.invoke(messageBuilder, outboundProperties);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private interface Attachment {

    CoreEvent addOutboundTo(CoreEvent event, String key);
  }
}
