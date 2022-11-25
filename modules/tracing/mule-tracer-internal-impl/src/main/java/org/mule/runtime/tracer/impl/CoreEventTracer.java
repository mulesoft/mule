/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.tracer.api.sniffer.SpanSnifferManager;
import org.mule.runtime.tracer.api.context.getter.DistributedTraceContextGetter;
import org.mule.runtime.tracer.api.EventTracer;
import org.mule.runtime.tracer.api.span.info.StartSpanInfo;
import org.mule.runtime.tracer.api.span.validation.Assertion;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.impl.span.factory.EventSpanFactory;
import org.mule.runtime.tracer.impl.span.method.AddSpanAttributeMethod;
import org.mule.runtime.tracer.impl.span.method.DefaultTracingMethods;
import org.mule.runtime.tracer.impl.span.method.EndEventSpanMethod;
import org.mule.runtime.tracer.impl.span.method.InjectDistributedTraceContextMethod;
import org.mule.runtime.tracer.impl.span.method.RecordSpanErrorMethod;
import org.mule.runtime.tracer.impl.span.method.SetCurrentSpanNameMethod;
import org.mule.runtime.tracer.impl.span.method.StartEventSpanMethod;
import org.mule.runtime.tracer.impl.span.method.GetTraceContextMapMethod;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A default implementation for a {@link CoreEventTracer}.
 *
 * @since 4.5.0
 */
public class CoreEventTracer implements EventTracer<CoreEvent>, Initialisable {

  @Inject
  private MuleContext muleContext;

  @Inject
  private EventSpanFactory eventSpanFactory;

  private StartEventSpanMethod<EventContext> startSpanMethod;
  private final EndEventSpanMethod<EventContext> endSpanCommand = DefaultTracingMethods.getEndSpanMethod();
  private final RecordSpanErrorMethod<EventContext> recordSpanErrorMethod = DefaultTracingMethods.getRecordSpanErrorMethod();
  private final SetCurrentSpanNameMethod<EventContext> setCurrentSpanNameMethod =
      DefaultTracingMethods.getSetCurrentSpanNameMethod();
  private final AddSpanAttributeMethod<EventContext> addSpanAttributeMethod = DefaultTracingMethods.getAddSpanAttributesMethod();
  private final InjectDistributedTraceContextMethod<EventContext> injectDistributedTraceContextMethod =
      DefaultTracingMethods.getInjectDistributedTraceContextMethod();
  private final GetTraceContextMapMethod<EventContext> getDistributedTraceContextMapMethod =
      DefaultTracingMethods.getDistributedTraceContextMapMethod();

  @Override
  public Optional<InternalSpan> startComponentSpan(CoreEvent coreEvent, StartSpanInfo spanCustomizationInfo) {
    return startSpanMethod.start(coreEvent.getContext(), coreEvent, spanCustomizationInfo);
  }

  @Override
  public void startComponentSpan(CoreEvent coreEvent, StartSpanInfo spanCustomizationInfo,
                                 Assertion assertion) {
    startSpanMethod.start(coreEvent.getContext(), coreEvent, spanCustomizationInfo, assertion);
  }

  @Override
  public void endCurrentSpan(CoreEvent coreEvent) {
    endSpanCommand.end(coreEvent.getContext());
  }

  @Override
  public void endCurrentSpan(CoreEvent coreEvent, Assertion condition) {
    endSpanCommand.end(coreEvent.getContext(), condition);
  }

  @Override
  public void injectDistributedTraceContext(EventContext eventContext,
                                            DistributedTraceContextGetter distributedTraceContextGetter) {
    injectDistributedTraceContextMethod.inject(eventContext, distributedTraceContextGetter);
  }

  @Override
  public void recordErrorAtCurrentSpan(CoreEvent coreEvent, Supplier<Error> errorSupplier, boolean isErrorEscapingCurrentSpan) {
    recordSpanErrorMethod.recordError(coreEvent.getContext(), errorSupplier, isErrorEscapingCurrentSpan,
                                      coreEvent.getFlowCallStack());
  }

  @Override
  public void setCurrentSpanName(CoreEvent coreEvent, String name) {
    setCurrentSpanNameMethod.setCurrentSpanName(coreEvent.getContext(), name);
  }

  @Override
  public void addCurrentSpanAttribute(CoreEvent coreEvent, String key, String value) {
    addSpanAttributeMethod.addAttribute(coreEvent.getContext(), key, value);
  }

  @Override
  public void addCurrentSpanAttributes(CoreEvent coreEvent, Map<String, String> attributes) {
    addSpanAttributeMethod.addAttributes(coreEvent.getContext(), attributes);
  }

  @Override
  public SpanSnifferManager getSpanExporterManager() {
    return eventSpanFactory.getSpanSnifferManager();
  }

  @Override
  public Map<String, String> getDistributedTraceContextMap(CoreEvent event) {
    return getDistributedTraceContextMapMethod.getDistributedTraceContextMap(event.getContext());
  }

  @Override
  public void initialise() throws InitialisationException {
    startSpanMethod = DefaultTracingMethods.getStartEventSpanMethod(muleContext.getConfiguration(), muleContext.getArtifactType(),
                                                                    muleContext.getConfiguration().getId(), eventSpanFactory);
  }

}
