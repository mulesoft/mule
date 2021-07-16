/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.diagnostics;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.PS_FLOW_END;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.PS_FLOW_DISPATCH;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.PS_FLOW_MESSAGE_PASSING;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.OPERATION_EXECUTED;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.PS_SCHEDULING_OPERATION_EXECUTION;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.STARTING_OPERATION_EXECUTION;
import static java.lang.String.format;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.diagnostics.producer.ComponentProcessingStratetegyProfilingDataProducer;

import java.util.HashMap;
import java.util.Map;


/**
 * Default diagnostic service for the runtime
 *
 * @since 4.4.0
 */
public class DefaultDiagnosticsService extends AbstractDiagnosticsService {

  protected Map<ProfilingEventType, ProfilingDataProducer> profilerDataProducers =
      new HashMap<ProfilingEventType, ProfilingDataProducer>() {

        {
          put(PS_FLOW_END,
              new ComponentProcessingStratetegyProfilingDataProducer(DefaultDiagnosticsService.this, PS_FLOW_END));
          put(PS_FLOW_DISPATCH,
              new ComponentProcessingStratetegyProfilingDataProducer(DefaultDiagnosticsService.this, PS_FLOW_DISPATCH));
          put(PS_FLOW_MESSAGE_PASSING,
              new ComponentProcessingStratetegyProfilingDataProducer(DefaultDiagnosticsService.this, PS_FLOW_MESSAGE_PASSING));
          put(OPERATION_EXECUTED,
              new ComponentProcessingStratetegyProfilingDataProducer(DefaultDiagnosticsService.this, OPERATION_EXECUTED));
          put(PS_SCHEDULING_OPERATION_EXECUTION,
              new ComponentProcessingStratetegyProfilingDataProducer(DefaultDiagnosticsService.this,
                                                                     PS_SCHEDULING_OPERATION_EXECUTION));
          put(STARTING_OPERATION_EXECUTION,
              new ComponentProcessingStratetegyProfilingDataProducer(DefaultDiagnosticsService.this,
                                                                     STARTING_OPERATION_EXECUTION));
        }

      };

  @Override
  public ProfilingDataProducer getProfilingDataProducer(ProfilingEventType profilingEventType) {
    if (!profilerDataProducers.containsKey(profilingEventType)) {
      throw new MuleRuntimeException((createStaticMessage(format(
                                                                 "Profiling event type not registered: %s",
                                                                 profilingEventType))));
    }
    return profilerDataProducers.get(profilingEventType);
  }

  @Override
  public ProfilerDataConsumerDiscoveryStrategy getDiscoveryStrategy() {
    return new DefaultProfilerDataConsumerDiscoveryStrategy();
  }

}
