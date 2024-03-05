/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.flow;

import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.TypedComponentIdentifier.builder;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.internal.util.message.ItemSequenceInfoUtils.fromGroupCorrelation;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.from;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import static org.mockito.Mockito.spy;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.ItemSequenceInfo;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.message.GroupCorrelation;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.mockito.Mockito;

/**
 * Provides a fluent API for building events for testing.
 */
public class TestEventBuilder {

  private Object payload;
  private Object attributes;
  private MediaType mediaType = MediaType.ANY;

  private String sourceCorrelationId = null;
  private ItemSequenceInfo itemSequenceInfo;

  private final Map<String, TypedValue> variables = new HashMap<>();

  private Function<Message, Message> spyMessage = input -> input;
  private Function<CoreEvent, CoreEvent> spyEvent = input -> input;

  private CompletableFuture<Void> externalCompletionCallback;

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
   * @deprecated use {@link #withItemSequenceInfo(ItemSequenceInfo)} instead
   */
  @Deprecated
  public TestEventBuilder withCorrelation(GroupCorrelation groupCorrelation) {
    return withItemSequenceInfo(fromGroupCorrelation(groupCorrelation));
  }

  /**
   * Configures the product event to have the provided {@code itemSequenceInfo}. See {@link CoreEvent#getItemSequenceInfo()}.
   *
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder withItemSequenceInfo(ItemSequenceInfo itemSequenceInfo) {
    this.itemSequenceInfo = itemSequenceInfo;

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
    variables.put(key, new TypedValue(value, null));

    return this;
  }

  /**
   * Prepares a flow variable with the given key and value to be set in the product.
   *
   * @param key      the key of the flow variable to put
   * @param value    the value of the flow variable to put
   * @param dataType the data type of the variable
   * @return this {@link TestEventBuilder}
   */
  public TestEventBuilder withVariable(String key, Object value, DataType dataType) {
    variables.put(key, new TypedValue(value, dataType));

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

  public TestEventBuilder setExternalCompletionCallback(CompletableFuture<Void> externalCompletionCallback) {
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

    if (attributes != null) {
      messageBuilder.attributesValue(attributes);
    }
    final Message muleMessage = messageBuilder.build();

    EventContext eventContext = getEventContext(flow);

    CoreEvent.Builder builder = CoreEvent.builder(eventContext)
        .message(spyMessage.apply(muleMessage)).itemSequenceInfo(ofNullable(itemSequenceInfo));
    for (Entry<String, TypedValue> variableEntry : variables.entrySet()) {
      builder.addVariable(variableEntry.getKey(), variableEntry.getValue().getValue(), variableEntry.getValue().getDataType());
    }
    CoreEvent event = builder.build();

    return spyEvent.apply(event);
  }

  private EventContext getEventContext(FlowConstruct flow) {
    EventContext eventContext;
    DefaultComponentLocation location = ((DefaultComponentLocation) from(flow.getName())).appendProcessorsPart()
        .appendLocationPart("0", of(builder()
            .type(OPERATION)
            .identifier(buildFromStringRepresentation("test"))
            .build()), empty(), OptionalInt.empty(), OptionalInt.empty());
    if (externalCompletionCallback != null) {
      eventContext = create(flow, location, sourceCorrelationId, of(externalCompletionCallback));
    } else {
      eventContext = create(flow, location, sourceCorrelationId);
    }
    return eventContext;
  }

}
