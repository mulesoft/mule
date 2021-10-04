/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.producer;

import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ExtensionProfilingEventContext;
import org.mule.runtime.core.internal.profiling.DefaultProfilingService;

/**
 * A {@link ProfilingDataProducer} for producing data related to extensions.
 *
 * @see org.mule.runtime.api.profiling.type.ExtensionProfilingEventType
 *
 * @since 4.4
 */
public class ExtensionProfilingDataProducer
    implements ProfilingDataProducer<ExtensionProfilingEventContext> {

  private final DefaultProfilingService defaultProfilingService;
  private final ProfilingEventType<ExtensionProfilingEventContext> profilingEventType;

  public ExtensionProfilingDataProducer(DefaultProfilingService defaultProfilingService,
                                        ProfilingEventType<ExtensionProfilingEventContext> profilingEventType) {
    this.defaultProfilingService = defaultProfilingService;
    this.profilingEventType = profilingEventType;
  }

  @Override
  public void triggerProfilingEvent(ExtensionProfilingEventContext profilingEventContext) {
    defaultProfilingService.notifyEvent(profilingEventContext, profilingEventType);
  }
}
