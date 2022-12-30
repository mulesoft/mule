/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl;

import static org.mule.runtime.tracer.api.span.validation.Assertion.SUCCESSFUL_ASSERTION;
import static org.mule.runtime.tracer.impl.SpanInfoUtils.enrichInitialSpanInfo;
import static org.mule.runtime.tracer.impl.span.command.EventContextAddAttributeCommand.getEventContextAddSpanAttributeCommandFrom;
import static org.mule.runtime.tracer.impl.span.command.EventContextAddAttributesCommand.getEventContextAddSpanAttributesCommandFrom;
import static org.mule.runtime.tracer.impl.span.command.EventContextEndSpanCommand.getEventContextEndSpanCommandFrom;
import static org.mule.runtime.tracer.impl.span.command.EventContextGetDistributedTraceContextMapCommand.getEventContextGetDistributedTraceContextMapCommand;
import static org.mule.runtime.tracer.impl.span.command.EventContextInjectDistributedTraceContextCommand.getEventContextInjectDistributedTraceContextCommand;
import static org.mule.runtime.tracer.impl.span.command.EventContextRecordErrorCommand.getEventContextRecordErrorCommand;
import static org.mule.runtime.tracer.impl.span.command.EventContextSetCurrentSpanNameCommand.getEventContextSetCurrentSpanNameCommand;
import static org.mule.runtime.tracer.impl.span.command.EventContextStartSpanCommand.getEventContextStartSpanCommandFrom;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.tracer.api.sniffer.SpanSnifferManager;
import org.mule.runtime.tracer.api.context.getter.DistributedTraceContextGetter;
import org.mule.runtime.tracer.api.EventTracer;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.api.span.validation.Assertion;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.impl.span.factory.EventSpanFactory;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Inject;

/**
 * A default implementation for a {@link CoreEventTracer}.
 *
 * @since 4.5.0
 */
public class CoreEventTracer implements EventTracer<CoreEvent> {

  @Inject
  private EventSpanFactory eventSpanFactory;

  @Override
  public Optional<InternalSpan> startComponentSpan(CoreEvent coreEvent, InitialSpanInfo spanCustomizationInfo) {
    return startComponentSpan(coreEvent, spanCustomizationInfo, SUCCESSFUL_ASSERTION);
  }

  @Override
  public Optional<InternalSpan> startComponentSpan(CoreEvent coreEvent, InitialSpanInfo initialSpanInfo,
                                                   Assertion assertion) {
    return getEventContextStartSpanCommandFrom(coreEvent.getContext(),
                                               eventSpanFactory,
                                               enrichInitialSpanInfo(initialSpanInfo, coreEvent),
                                               assertion).execute();
  }

  @Override
  public void endCurrentSpan(CoreEvent coreEvent) {
    endCurrentSpan(coreEvent, SUCCESSFUL_ASSERTION);
  }

  @Override
  public void endCurrentSpan(CoreEvent coreEvent, Assertion condition) {
    getEventContextEndSpanCommandFrom(coreEvent.getContext(), SUCCESSFUL_ASSERTION).execute();
  }

  @Override
  public void injectDistributedTraceContext(EventContext eventContext,
                                            DistributedTraceContextGetter distributedTraceContextGetter) {
    getEventContextInjectDistributedTraceContextCommand(eventContext, distributedTraceContextGetter).execute();
  }

  @Override
  public void recordErrorAtCurrentSpan(CoreEvent coreEvent, Supplier<Error> errorSupplier, boolean isErrorEscapingCurrentSpan) {
    getEventContextRecordErrorCommand(coreEvent.getContext(),
                                      errorSupplier,
                                      isErrorEscapingCurrentSpan,
                                      coreEvent.getFlowCallStack()).execute();
  }

  @Override
  public void setCurrentSpanName(CoreEvent coreEvent, String name) {
    getEventContextSetCurrentSpanNameCommand(coreEvent.getContext(), name).execute();
  }

  @Override
  public void addCurrentSpanAttribute(CoreEvent coreEvent, String key, String value) {
    getEventContextAddSpanAttributeCommandFrom(coreEvent.getContext(), key, value).execute();
  }

  @Override
  public void addCurrentSpanAttributes(CoreEvent coreEvent, Map<String, String> attributes) {
    getEventContextAddSpanAttributesCommandFrom(coreEvent.getContext(), attributes).execute();
  }

  @Override
  public SpanSnifferManager getSpanExporterManager() {
    return eventSpanFactory.getSpanSnifferManager();
  }

  @Override
  public Map<String, String> getDistributedTraceContextMap(CoreEvent event) {
    return getEventContextGetDistributedTraceContextMapCommand(event.getContext()).execute();
  }

}
