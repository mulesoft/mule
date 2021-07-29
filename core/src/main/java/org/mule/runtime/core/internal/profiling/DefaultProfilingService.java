/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.FLOW_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.OPERATION_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_FLOW_MESSAGE_PASSING;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_OPERATION_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_OPERATION_EXECUTION;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.profiling.ProfilingDataConsumerDiscoveryStrategy;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingEventContext;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.core.internal.profiling.producer.ComponentProcessingStrategyProfilingDataProducer;

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

  protected Map<ProfilingEventType<? extends ProfilingEventContext>, ProfilingDataProducer<?>> profilingDataProducers =
      new HashMap() {

        // TODO MULE-19596 replace this map when the discovery strategy based on server plugins is implemented.
        {
          put(FLOW_EXECUTED,
              new ComponentProcessingStrategyProfilingDataProducer(DefaultProfilingService.this, FLOW_EXECUTED));
          put(PS_SCHEDULING_FLOW_EXECUTION,
              new ComponentProcessingStrategyProfilingDataProducer(DefaultProfilingService.this, PS_SCHEDULING_FLOW_EXECUTION));
          put(STARTING_FLOW_EXECUTION,
              new ComponentProcessingStrategyProfilingDataProducer(DefaultProfilingService.this, STARTING_FLOW_EXECUTION));
          put(PS_FLOW_MESSAGE_PASSING,
              new ComponentProcessingStrategyProfilingDataProducer(DefaultProfilingService.this, PS_FLOW_MESSAGE_PASSING));
          put(OPERATION_EXECUTED,
              new ComponentProcessingStrategyProfilingDataProducer(DefaultProfilingService.this, OPERATION_EXECUTED));
          put(PS_SCHEDULING_OPERATION_EXECUTION,
              new ComponentProcessingStrategyProfilingDataProducer(DefaultProfilingService.this,
                                                                   PS_SCHEDULING_OPERATION_EXECUTION));
          put(STARTING_OPERATION_EXECUTION,
              new ComponentProcessingStrategyProfilingDataProducer(DefaultProfilingService.this,
                                                                   STARTING_OPERATION_EXECUTION));
        }
      };

  @Override
  public ProfilingDataConsumerDiscoveryStrategy getDiscoveryStrategy() {
    return new DefaultProfilingDataConsumerDiscoveryStrategy();
  }

  @Override
  public <T extends ProfilingEventContext> ProfilingDataProducer<T> getProfilingDataProducer(
                                                                                             ProfilingEventType<T> profilingEventType) {
    if (!profilingDataProducers.containsKey(profilingEventType)) {
      throw new MuleRuntimeException((createStaticMessage(format("Profiling event type not registered: %s",
                                                                 profilingEventType))));
    }
    return (ProfilingDataProducer<T>) profilingDataProducers.get(profilingEventType);
  }

  @Override
  public <T extends ProfilingEventContext> void registerProfilingDataProducer(ProfilingEventType<T> profilingEventType,
                                                                              ProfilingDataProducer<T> profilingDataProducer) {
    profilingDataProducers.put(profilingEventType, profilingDataProducer);
  }
}
