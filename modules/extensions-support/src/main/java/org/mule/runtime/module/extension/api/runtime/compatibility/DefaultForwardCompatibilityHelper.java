/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.api.runtime.compatibility;

import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverUtils.resolveDistributedTraceContextManager;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.InternalProfilingService;
import org.mule.runtime.extension.api.runtime.parameter.CorrelationInfo;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.runtime.module.extension.internal.runtime.parameter.ImmutableCorrelationInfo;
import org.mule.runtime.module.extension.internal.runtime.source.legacy.LegacySourceCallbackContextAdapter;

import org.mule.runtime.tracer.api.EventTracer;
import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;
import org.mule.sdk.compatibility.api.utils.ForwardCompatibilityHelper;

import javax.inject.Inject;

public class DefaultForwardCompatibilityHelper implements ForwardCompatibilityHelper {

  @Inject
  private InternalProfilingService profilingService;

  @Override
  public DistributedTraceContextManager getDistributedTraceContextManager(SourceCallbackContext sourceCallbackContext) {
    if (sourceCallbackContext instanceof LegacySourceCallbackContextAdapter) {
      return ((LegacySourceCallbackContextAdapter) sourceCallbackContext).getDistributedSourceTraceContext();
    } else {
      throw new IllegalStateException("Source Callback Context does not posses a Distributed Source Trace Context");
    }
  }

  public DistributedTraceContextManager getDistributedTraceContextManager(CorrelationInfo correlationInfo) {
    return getDistributedTraceContextManager((org.mule.sdk.api.runtime.parameter.CorrelationInfo) correlationInfo);
  }

  public DistributedTraceContextManager getDistributedTraceContextManager(org.mule.sdk.api.runtime.parameter.CorrelationInfo correlationInfo) {
    if (correlationInfo instanceof ImmutableCorrelationInfo) {
      ImmutableCorrelationInfo immutableCorrelationInfo = ((ImmutableCorrelationInfo) correlationInfo);
      // TODO: W-13837896: we have to verify here if we want to trace the operations that are invoked through the extensions
      // client.
      EventTracer<CoreEvent> coreEventEventTracer =
          immutableCorrelationInfo.getCoreEventEventTracer().orElse(profilingService.getCoreEventTracer());
      return resolveDistributedTraceContextManager((immutableCorrelationInfo.getEvent()),
                                                   coreEventEventTracer);
    } else {
      throw new IllegalStateException("The given Correlation Info does not posses a Distributed Source Trace Context");
    }
  }
}
