/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling;

import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingEventContext;
import org.mule.runtime.api.profiling.ProfilingProducerScope;
import org.mule.runtime.api.profiling.threading.ThreadSnapshotCollector;
import org.mule.runtime.api.profiling.tracing.ExecutionContext;
import org.mule.runtime.api.profiling.tracing.TracingService;
import org.mule.runtime.api.profiling.type.ProfilingEventType;

import org.mule.runtime.core.api.event.CoreEvent;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.mule.runtime.tracer.api.EventTracer;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.runtime.tracer.api.sniffer.SpanSnifferManager;
import org.mule.runtime.tracer.api.context.getter.DistributedTraceContextGetter;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.info.StartSpanInfo;
import org.mule.runtime.tracer.api.span.validation.Assertion;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static java.util.Collections.emptySet;
import static java.util.Optional.empty;

/**
 * A Profiling Service that disables all data production. The {@link ProfilingDataProducer} implements operations that do not
 * propagate the profiling data.
 *
 * @since 4.5.0
 */
public class NoOpProfilingService implements InternalProfilingService, PrivilegedProfilingService {

  private final EventTracer<CoreEvent> eventTracer = new EventTracer<CoreEvent>() {

    @Override
    public Optional<InternalSpan> startComponentSpan(CoreEvent coreEvent, StartSpanInfo spanCustomizationInfo) {
      return empty();
    }

    @Override
    public Optional<InternalSpan> startComponentSpan(CoreEvent coreEvent, StartSpanInfo startSpanInfo,
                                                     Assertion assertion) {
      return empty();
    }

    @Override
    public void endCurrentSpan(CoreEvent coreEvent) {

    }

    @Override
    public void endCurrentSpan(CoreEvent coreEvent, Assertion condition) {

    }

    @Override
    public void injectDistributedTraceContext(EventContext eventContext,
                                              DistributedTraceContextGetter distributedTraceContextGetter) {

    }

    @Override
    public void recordErrorAtCurrentSpan(CoreEvent coreEvent, Supplier<Error> errorSupplier, boolean isErrorEscapingCurrentSpan) {

    }

    @Override
    public void setCurrentSpanName(CoreEvent coreEvent, String name) {

    }

    @Override
    public void addCurrentSpanAttribute(CoreEvent coreEvent, String key, String value) {

    }

    @Override
    public void addCurrentSpanAttributes(CoreEvent coreEvent, Map<String, String> attributes) {

    }

    @Override
    public SpanSnifferManager getSpanExporterManager() {
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
    };
  };

  private final TracingService noOpTracingService = new TracingService() {

    @Override
    public ExecutionContext getCurrentExecutionContext() {
      return null;
    }

    @Override
    public void deleteCurrentExecutionContext() {
      // No op
    }

    @Override
    public ExecutionContext setCurrentExecutionContext(ExecutionContext executionContext) {
      return null;
    }

  };

  @SuppressWarnings("rawtypes")
  private final ProfilingDataProducer<?, ?> profilingDataProducer = new ProfilingDataProducer() {


    @Override
    public void triggerProfilingEvent(ProfilingEventContext profilerEventContext) {
      // No op
    }

    @Override
    public void triggerProfilingEvent(Object sourceData, Function transformation) {
      // No op
    }
  };

  @SuppressWarnings("unchecked")
  @Override
  public <T extends ProfilingEventContext, S> ProfilingDataProducer<T, S> getProfilingDataProducer(
                                                                                                   ProfilingEventType<T> profilingEventType) {
    return (ProfilingDataProducer<T, S>) profilingDataProducer;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends ProfilingEventContext, S> ProfilingDataProducer<T, S> getProfilingDataProducer(
                                                                                                   ProfilingEventType<T> profilingEventType,
                                                                                                   ProfilingProducerScope producerContext) {
    return (ProfilingDataProducer<T, S>) profilingDataProducer;
  }

  @Override
  public <T extends ProfilingEventContext, S> void registerProfilingDataProducer(ProfilingEventType<T> profilingEventType,
                                                                                 ProfilingDataProducer<T, S> profilingDataProducer) {
    // Nothing to do
  }

  @Override
  public <T extends ProfilingEventContext> void registerProfilingDataConsumer(ProfilingDataConsumer<T> profilingDataConsumer) {
    // Nothing to do
  }

  @Override
  public void injectDistributedTraceContext(EventContext eventContext,
                                            DistributedTraceContextGetter distributedTraceContextGetter) {

  }

  @Override
  public ThreadSnapshotCollector getThreadSnapshotCollector() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TracingService getTracingService() {
    return noOpTracingService;
  }

  @Override
  public <T extends ProfilingEventContext, S> Mono<S> enrichWithProfilingEventMono(Mono<S> original,
                                                                                   ProfilingDataProducer<T, S> dataProducer,
                                                                                   Function<S, T> transformer) {
    return original;
  }

  @Override
  public <T extends ProfilingEventContext, S> Flux<S> enrichWithProfilingEventFlux(Flux<S> original,
                                                                                   ProfilingDataProducer<T, S> dataProducer,
                                                                                   Function<S, T> transformer) {
    return original;
  }

  @Override
  public <S> Mono<S> setCurrentExecutionContext(Mono<S> original, Function<S, ExecutionContext> executionContextSupplier) {
    return original;
  }

  @Override
  public <S> Flux<S> setCurrentExecutionContext(Flux<S> original, Function<S, ExecutionContext> executionContextSupplier) {
    return original;
  }

  @Override
  public EventTracer<CoreEvent> getCoreEventTracer() {
    return eventTracer;
  }

}
