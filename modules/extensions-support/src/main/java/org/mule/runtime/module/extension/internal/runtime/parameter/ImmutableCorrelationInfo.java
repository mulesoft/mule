/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.parameter;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.internal.profiling.NoopCoreEventTracer.getNoopCoreEventTracer;

import org.mule.runtime.api.message.ItemSequenceInfo;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.parameter.CorrelationInfo;
import org.mule.runtime.tracer.api.EventTracer;

import java.util.Optional;

/**
 * Immutable implementation of {@link CorrelationInfo}
 *
 * @since 4.1
 */
public class ImmutableCorrelationInfo implements CorrelationInfo {

  private final String eventId;
  private final boolean outboundCorrelationEnabled;
  private final String correlationId;
  private final ItemSequenceInfo itemSequenceInfo;
  private final CoreEvent event;
  private EventTracer<CoreEvent> coreEventEventTracer = getNoopCoreEventTracer();


  public ImmutableCorrelationInfo(String eventId, boolean outboundCorrelationEnabled, String correlationId,
                                  ItemSequenceInfo itemSequenceInfo, CoreEvent event) {
    this.eventId = eventId;
    this.outboundCorrelationEnabled = outboundCorrelationEnabled;
    this.correlationId = correlationId;
    this.itemSequenceInfo = itemSequenceInfo;
    this.event = event;
  }

  public ImmutableCorrelationInfo(String eventId, boolean outboundCorrelationEnabled, String correlationId,
                                  ItemSequenceInfo itemSequenceInfo, CoreEvent event,
                                  EventTracer<CoreEvent> coreEventEventTracer) {
    this.eventId = eventId;
    this.outboundCorrelationEnabled = outboundCorrelationEnabled;
    this.correlationId = correlationId;
    this.itemSequenceInfo = itemSequenceInfo;
    this.event = event;
    this.coreEventEventTracer = coreEventEventTracer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEventId() {
    return eventId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isOutboundCorrelationEnabled() {
    return outboundCorrelationEnabled;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getCorrelationId() {
    return correlationId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ItemSequenceInfo> getItemSequenceInfo() {
    return ofNullable(itemSequenceInfo);
  }

  /**
   * @return The {@link CoreEvent} being processed
   */
  public CoreEvent getEvent() {
    return event;
  }

  public EventTracer<CoreEvent> getCoreEventEventTracer() {
    return coreEventEventTracer;
  }
}
