/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.profiling.ProfilerDataConsumerDiscoveryStrategy;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingEventContext;
import org.mule.runtime.api.profiling.type.ProfilingEventType;

import java.util.HashMap;
import java.util.Map;

/**
 * Default diagnostic service for the runtime.
 * <p>
 * This is based on the Notification API.
 *
 * @since 4.4
 */
public class DefaultProfilingService extends AbstractProfilingService {

  private final Map<ProfilingEventType<? extends ProfilingEventContext>, ProfilingDataProducer<?>> profilerDataProducers =
      new HashMap<>();

  @Override
  public ProfilerDataConsumerDiscoveryStrategy getDiscoveryStrategy() {
    return new DefaultProfilerDataConsumerDiscoveryStrategy();
  }

  @Override
  public <T extends ProfilingEventContext> ProfilingDataProducer<T> getProfilingDataProducer(
                                                                                             ProfilingEventType<T> profilingEventType) {
    if (!profilerDataProducers.containsKey(profilingEventType)) {
      throw new MuleRuntimeException((createStaticMessage(format("Profiling event type not registered: %s",
                                                                 profilingEventType))));
    }
    return (ProfilingDataProducer<T>) profilerDataProducers.get(profilingEventType);
  }

  @Override
  public <T extends ProfilingEventContext> void registerProfilingDataProducer(ProfilingEventType<T> profilingEventType,
                                                                              ProfilingDataProducer<T> profilingDataProducer) {
    profilerDataProducers.put(profilingEventType, profilingDataProducer);
  }
}
