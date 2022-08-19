/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.runtime.compatibility;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.InternalProfilingService;
import org.mule.runtime.extension.api.runtime.parameter.CorrelationInfo;
import org.mule.runtime.module.extension.internal.runtime.parameter.ImmutableCorrelationInfo;
import org.mule.runtime.module.extension.internal.runtime.resolver.DistributedTraceContextManagerResolver;
import org.mule.runtime.module.extension.internal.runtime.source.trace.DefaultDistributedSourceTraceContext;
import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;
import org.mule.sdk.compatibility.api.utils.ForwardCompatibilityHelper;

import javax.inject.Inject;

public class DefaultForwardCompatibilityHelper implements ForwardCompatibilityHelper {

  @Inject
  private InternalProfilingService profilingService;

  public DistributedTraceContextManager getDistributedTraceContextManager(CorrelationInfo correlationInfo) {
    return getDistributedTraceContextManager(correlationInfo);
  }

  public DistributedTraceContextManager getDistributedTraceContextManager(org.mule.sdk.api.runtime.parameter.CorrelationInfo correlationInfo) {
    if (correlationInfo instanceof ImmutableCorrelationInfo) {
      CoreEvent event = ((ImmutableCorrelationInfo) correlationInfo).getEvent();
      DistributedTraceContextManagerResolver argumentResolver =
          new DistributedTraceContextManagerResolver(profilingService.getCoreEventTracer());
      return argumentResolver.resolve(event);
    } else {
      return new DefaultDistributedSourceTraceContext();
    }
  }
}
