/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.flow;

import static java.lang.String.format;
import static org.junit.Assert.fail;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.message.GroupCorrelation;

import org.mockito.Mockito;

import java.io.Serializable;
import java.util.Map;

import javax.activation.DataHandler;

/**
 * Provides a fluent API for running events through batch FlowConstructs.
 *
 * @param <R> the runner class for a {@link FlowConstruct} implementation.
 */
public abstract class FlowConstructRunner<R extends FlowConstructRunner> {

  protected Registry registry;
  protected TestEventBuilder eventBuilder = new TestEventBuilder();
  private CoreEvent requestEvent;

  /**
   * Prepares the given data to be sent as the payload of the {@link CoreEvent} to the configured flow.
   *
   * @param payload the payload to use in the message
   * @return this {@link FlowRunner}
   */
  public R withPayload(Object payload) {
    eventBuilder.withPayload(payload);

    return (R) this;
  }

  /**
   * Prepares the given data to be sent as the mediaType of the payload of the {@link CoreEvent} to the configured flow.
   *
   * @param mediaType the mediaType to use in the message
   * @return this {@link FlowRunner}
   */
  public R withMediaType(MediaType mediaType) {
    eventBuilder.withMediaType(mediaType);

    return (R) this;
  }

  /**
   * Prepares the given data to be sent as the attributes of the {@link CoreEvent} to the configured flow.
   *
   * @param attributes the message attributes
   * @return this {@link FlowRunner}
   */
  public R withAttributes(Object attributes) {
    eventBuilder.withAttributes(attributes);
    return (R) this;
  }

  /**
   * Prepares a property with the given key and value to be sent as an inbound property of the {@link Message} to the configured
   * flow.
   *
   * @param key the key of the inbound property to add
   * @param value the value of the inbound property to add
   * @return this {@link FlowRunner}
   * @deprecated Transport infrastructure is deprecated. Use {@link Message#getAttributes()} instead.
   */
  @Deprecated
  public R withInboundProperty(String key, Serializable value) {
    eventBuilder.withInboundProperty(key, value);

    return (R) this;
  }

  /**
   * Prepares the given properties map to be sent as inbound properties of the {@link Message} to the configured flow.
   *
   * @param properties the inbound properties to add
   * @return this {@link FlowRunner}
   * @deprecated Transport infrastructure is deprecated. Use {@link Message#getAttributes()} instead.
   */
  @Deprecated
  public R withInboundProperties(Map<String, Serializable> properties) {
    eventBuilder.withInboundProperties(properties);

    return (R) this;
  }

  /**
   * Prepares a property with the given key and value to be sent as an outbound property of the {@link Message} to the configured
   * flow.
   *
   * @param key the key of the outbound property to add
   * @param value the value of the outbound property to add
   * @return this {@link FlowRunner}
   * @deprecated Transport infrastructure is deprecated. Use {@link Message#getAttributes()} instead.
   */
  @Deprecated
  public R withOutboundProperty(String key, Serializable value) {
    eventBuilder.withOutboundProperty(key, value);

    return (R) this;
  }

  /**
   * Prepares an attachment with the given key and value to be sent in the {@link Message} to the configured flow.
   *
   * @param key the key of the attachment to add
   * @param value the {@link DataHandler} for the attachment to add
   * @return this {@link FlowRunner}
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
  public R withInboundAttachment(String key, DataHandler value) {
    eventBuilder.withInboundAttachment(key, value);

    return (R) this;
  }

  /**
   * Prepares a property with the given key and value to be sent as a session property of the {@link Message} to the configured
   * flow.
   *
   * @param key the key of the session property to add
   * @param value the value of the session property to add
   * @return this {@link FlowRunner}
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
  public R withSessionProperty(String key, Object value) {
    eventBuilder.withSessionProperty(key, value);

    return (R) this;
  }

  /**
   * Configures the product event to have the provided {@code sourceCorrelationId}. See {@link CoreEvent#getCorrelationId()}.
   *
   * @return this {@link TestEventBuilder}
   */
  public R withSourceCorrelationId(String sourceCorrelationId) {
    eventBuilder.withSourceCorrelationId(sourceCorrelationId);

    return (R) this;
  }

  /**
   * Configures the product event to have the provided {@code correlation}. See {@link CoreEvent#getGroupCorrelation()}.
   *
   * @return this {@link TestEventBuilder}
   */
  public R withCorrelation(GroupCorrelation correlation) {
    eventBuilder.withCorrelation(correlation);

    return (R) this;
  }

  /**
   * Prepares a flow variable with the given key and value to be set in the {@link Message} to the configured flow.
   *
   * @param key the key of the flow variable to put
   * @param value the value of the flow variable to put
   * @return this {@link FlowRunner}
   */
  public R withVariable(String key, Object value) {
    eventBuilder.withVariable(key, value);

    return (R) this;
  }

  /**
   * Prepares a flow variable with the given key and value to be set in the {@link Message} to the configured flow.
   *
   * @param key the key of the flow variable to put
   * @param value the value of the flow variable to put
   * @param dataType the value data type
   * @return this {@link FlowRunner}
   */
  public R withVariable(String key, Object value, DataType dataType) {
    eventBuilder.withVariable(key, value, dataType);

    return (R) this;
  }

  /**
   * Will spy the built {@link Message} and {@link CoreEvent}. See {@link Mockito#spy(Object) spy}.
   *
   * @return this {@link FlowRunner}
   */
  public R spyObjects() {
    eventBuilder.spyObjects();

    return (R) this;
  }

  /**
   * Initializes this runner with a mule context.
   *
   * @param registry the registry for the currently running test
   */
  public FlowConstructRunner(Registry registry) {
    this.registry = registry;
  }

  protected CoreEvent getOrBuildEvent() {
    if (requestEvent == null) {
      doBuildEvent();
    }
    return requestEvent;
  }

  /**
   * Builds a new event based on this runner's config. If one has already been built, it will fail.
   *
   * @return an event that would be used to go through the flow.
   */
  public CoreEvent buildEvent() {
    if (requestEvent == null) {
      doBuildEvent();
      return requestEvent;
    } else {
      fail("An event has already been build. Maybe you forgot to call reset()?");
      return null;
    }
  }

  private void doBuildEvent() {
    FlowConstruct flow = getFlowConstruct();
    requestEvent = eventBuilder.build(flow);
  }

  protected FlowConstruct getFlowConstruct() {
    return registry.<FlowConstruct>lookupByName(getFlowConstructName())
        .orElseThrow(() -> new NullPointerException(format("No flow with name '%s' found.", getFlowConstructName())));
  }

  /**
   * Clears the last built requestEvent, allowing for reuse of this runner.
   */
  public void reset() {
    requestEvent = null;
  }

  /**
   * @return the name of the {@link FlowConstruct} to use.
   */
  public abstract String getFlowConstructName();

}
