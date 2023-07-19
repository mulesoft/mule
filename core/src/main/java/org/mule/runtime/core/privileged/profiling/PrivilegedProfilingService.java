/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.profiling;

import static java.util.Collections.emptySet;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.ProfilingEventContext;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.tracer.api.context.getter.DistributedTraceContextGetter;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.runtime.tracer.api.sniffer.SpanSnifferManager;

import java.util.Collection;

/**
 * A {@link ProfilingService} that allows to perform some extra privileged operations.
 */
public interface PrivilegedProfilingService extends ProfilingService {

  /**
   * Registers a {@link ProfilingDataConsumer} dynamically.
   *
   * @param profilingDataConsumer the {@link ProfilingDataConsumer} to register.
   * @param <T>                   the {@link ProfilingEventContext} corresponding to the profiling event types the data consumer
   *                              listens to.
   */
  <T extends ProfilingEventContext> void registerProfilingDataConsumer(ProfilingDataConsumer<T> profilingDataConsumer);

  void injectDistributedTraceContext(EventContext eventContext,
                                     DistributedTraceContextGetter distributedTraceContextGetter);

  /**
   * @return gets an {@link SpanSnifferManager}.
   *
   *         This is used for capturing spans in privileged modules but should not be exposed as API.
   *
   * @since 4.5.0
   */
  default SpanSnifferManager getSpanExportManager() {
    return new SpanSnifferManager() {

      @Override
      public ExportedSpanSniffer getExportedSpanSniffer() {
        return new ExportedSpanSniffer() {

          @Override
          public Collection<CapturedExportedSpan> getExportedSpans() {
            return emptySet();
          }

          @Override
          public void dispose() {
            // Nothing to dispose.
          }
        };
      }
    };
  }
}
