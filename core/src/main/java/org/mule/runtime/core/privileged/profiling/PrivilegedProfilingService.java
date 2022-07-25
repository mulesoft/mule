/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.profiling;

import static java.util.Collections.emptySet;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.ProfilingEventContext;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.core.api.event.CoreEvent;

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

  /**
   * @return gets an {@link SpanExportManager}.
   *
   *         This is used for capturing spans in privileged modules but should not be exposed as API.
   *
   * @since 4.5.0
   */
  default SpanExportManager getSpanExportManager() {
    return new SpanExportManager() {

      @Override
      public ExportedSpanCapturer getExportedSpanCapturer() {
        return new ExportedSpanCapturer() {

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

  /**
   * Starts a component span. This is used as a privileged api only for testing.
   *
   * @param coreEvent the {@link CoreEvent} that has hit the {@link Component}
   * @param component the {@link Component} that was hit by the {@link CoreEvent}
   *
   *                  TODO: W-11486418 - Improve FlowRunner Creation of Spans
   *
   * @since 4.5.0
   */
  default void startComponentSpan(CoreEvent coreEvent, Component component) {}

  /**
   * End a component span. This is used as a privileged api only for testing.
   *
   * @param coreEvent the {@link CoreEvent}.
   *
   *                  TODO: W-11486418 - Improve FlowRunner Creation of Spans
   *
   * @since 4.5.0
   */
  default void endComponentSpan(CoreEvent coreEvent) {}
}
