/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.event.trace;

import static org.mule.runtime.core.internal.event.trace.extractor.RuntimeEventTraceExtractors.getDefaultBaggageExtractor;
import static org.mule.runtime.core.internal.event.trace.extractor.RuntimeEventTraceExtractors.getDefaultTraceContextFieldsExtractor;

import static java.util.Optional.ofNullable;

import org.mule.runtime.api.event.DistributedTraceContext;
import org.mule.runtime.core.internal.event.trace.extractor.TraceContextFieldExtractor;
import org.mule.sdk.api.runtime.source.SdkDistributedTraceContextMapGetter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link DistributedTraceContext} associated to an event. Represents the distributed context that was propagated to the
 * {@link org.mule.runtime.api.event.EventContext}
 *
 * @since 4.5.0
 */
public class EventDistributedTraceContext implements DistributedTraceContext {

  private final Map<String, String> tracingFields = new HashMap<>();
  private final Map<String, String> baggageItems = new HashMap<>();

  public static EventDistributedContextBuilder builder() {
    return new EventDistributedContextBuilder();
  }

  private EventDistributedTraceContext(TraceContextFieldExtractor tracingFieldExtractor,
                                       TraceContextFieldExtractor baggageItemsExtractor,
                                       SdkDistributedTraceContextMapGetter getter) {
    tracingFields.putAll(tracingFieldExtractor.extract(getter));
    baggageItems.putAll(baggageItemsExtractor.extract(getter));
  }

  @Override
  public Optional<String> getTraceFieldValue(String key) {
    return ofNullable(tracingFields.get(key));
  }

  @Override
  public Map<String, String> tracingFieldsAsMap() {
    return tracingFields;
  }

  @Override
  public Optional<String> getBaggageItem(String key) {
    return ofNullable(baggageItems.get(key));
  }

  @Override
  public Map<String, String> baggageItemsAsMap() {
    return baggageItems;
  }

  /**
   * Builder for {@link EventDistributedTraceContext}
   *
   * @since 4.5.0
   */
  public static final class EventDistributedContextBuilder {

    private SdkDistributedTraceContextMapGetter sdkDistributedTraceContextMapGetter;

    private EventDistributedContextBuilder() {}

    public EventDistributedContextBuilder withGetter(SdkDistributedTraceContextMapGetter sdkDistributedTraceContextMapGetter) {
      this.sdkDistributedTraceContextMapGetter = sdkDistributedTraceContextMapGetter;
      return this;
    }

    public DistributedTraceContext build() {
      return new EventDistributedTraceContext(getDefaultTraceContextFieldsExtractor(),
                                              getDefaultBaggageExtractor(),
                                              sdkDistributedTraceContextMapGetter);
    }
  }
}
