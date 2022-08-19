/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.parameter;

import static java.util.Optional.ofNullable;

import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.message.ItemSequenceInfo;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.parameter.CorrelationInfo;

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

  public ImmutableCorrelationInfo(String eventId, boolean outboundCorrelationEnabled, String correlationId,
                                  ItemSequenceInfo itemSequenceInfo, CoreEvent event) {
    this.eventId = eventId;
    this.outboundCorrelationEnabled = outboundCorrelationEnabled;
    this.correlationId = correlationId;
    this.itemSequenceInfo = itemSequenceInfo;
    this.event = event;
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
}
