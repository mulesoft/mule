/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl;

import static org.mule.runtime.api.config.MuleRuntimeFeature.PUT_TRACE_ID_AND_SPAN_ID_IN_MDC;
import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_PROPAGATION_OF_EXCEPTIONS_IN_TRACING;
import static org.mule.runtime.tracer.api.span.validation.Assertion.SUCCESSFUL_ASSERTION;
import static org.mule.runtime.tracer.impl.SpanInfoUtils.enrichInitialSpanInfo;
import static org.mule.runtime.tracer.impl.span.command.EventContextAddAttributeCommand.getEventContextAddAttributeCommand;
import static org.mule.runtime.tracer.impl.span.command.EventContextAddAttributesCommand.getEventContextAddAttributesCommand;
import static org.mule.runtime.tracer.impl.span.command.EventContextEndSpanCommand.getEventContextEndSpanCommandFrom;
import static org.mule.runtime.tracer.impl.span.command.EventContextGetDistributedTraceContextMapCommand.getEventContextGetDistributedTraceContextMapCommand;
import static org.mule.runtime.tracer.impl.span.command.EventContextInjectDistributedTraceContextCommand.getEventContextInjectDistributedTraceContextCommand;
import static org.mule.runtime.tracer.impl.span.command.EventContextRecordErrorCommand.getEventContextRecordErrorCommand;
import static org.mule.runtime.tracer.impl.span.command.EventContextSetCurrentSpanNameCommand.getEventContextSetCurrentSpanNameCommand;
import static org.mule.runtime.tracer.impl.span.command.EventContextStartSpanCommand.getEventContextStartSpanCommandFrom;

import static java.lang.Boolean.getBoolean;

import static org.slf4j.LoggerFactory.getLogger;


import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.tracer.api.sniffer.SpanSnifferManager;
import org.mule.runtime.tracer.api.context.getter.DistributedTraceContextGetter;
import org.mule.runtime.tracer.api.EventTracer;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.api.span.validation.Assertion;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.impl.span.command.EventContextAddAttributeCommand;
import org.mule.runtime.tracer.impl.span.command.EventContextAddAttributesCommand;
import org.mule.runtime.tracer.impl.span.command.EventContextEndSpanCommand;
import org.mule.runtime.tracer.impl.span.command.EventContextGetDistributedTraceContextMapCommand;
import org.mule.runtime.tracer.impl.span.command.EventContextInjectDistributedTraceContextCommand;
import org.mule.runtime.tracer.impl.span.command.EventContextRecordErrorCommand;
import org.mule.runtime.tracer.impl.span.command.EventContextSetCurrentSpanNameCommand;
import org.mule.runtime.tracer.impl.span.command.EventContextStartSpanCommand;
import org.mule.runtime.tracer.impl.span.factory.EventSpanFactory;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * A default implementation for a {@link CoreEventTracer}.
 *
 * @since 4.5.0
 */
public class CoreEventTracer implements EventTracer<CoreEvent>, Initialisable {

  private final boolean propagateTracingExceptions = getBoolean(ENABLE_PROPAGATION_OF_EXCEPTIONS_IN_TRACING);

  private static final Logger LOGGER = getLogger(CoreEventTracer.class);
  public static final String ERROR_ON_EXECUTING_CORE_EVENT_TRACER_START_COMMAND_MESSAGE =
      "Error on executing core event tracer start command";
  public static final String ERROR_ON_EXECUTING_CORE_EVENT_TRACER_END_COMMAND_MESSAGE =
      "Error on executing core event tracer end command";
  public static final String ERROR_ON_EXECUTING_CORE_EVENT_TRACER_INJECT_DISTRIBUTED_TRACE_CONTEXT_COMMAND_MESSAGE =
      "Error on executing core event tracer inject distributed trace context command";
  public static final String ERROR_ON_EXECUTING_CORE_EVENT_TRACER_RECORD_ERROR_COMMAND_MESSAGE =
      "Error on executing core event tracer record error command";
  public static final String ERROR_ON_EXECUTING_CORE_EVENT_TRACER_ADD_ATTRIBUTES_COMMAND_MESSAGE =
      "Error on executing core event tracer add attributes command";
  public static final String ERROR_ON_EXECUTING_CORE_EVENT_TRACER_ADD_ATTRIBUTE_COMMAND_MESSAGE =
      "Error on executing core event tracer add attribute command";
  public static final String ERROR_ON_EXECUTING_CORE_EVENT_SET_CURRENT_SPAN_COMMAND_MESSAGE =
      "Error on executing core event set current span command";
  public static final String ERROR_ON_EXECUTING_CORE_EVENT_GET_DISTRIBUTED_CONTEXT_SPAN_COMMAND_MESSAGE =
      "Error on executing core event get distributed context span command";

  @Inject
  private EventSpanFactory eventSpanFactory;

  @Inject
  FeatureFlaggingService featureFlaggingService;

  private EventContextStartSpanCommand startCommand;

  private EventContextEndSpanCommand endCommand;

  private EventContextInjectDistributedTraceContextCommand injectDistributedTraceContextCommand;

  private EventContextRecordErrorCommand recordErrorAtCurrentSpanCommand;

  private EventContextAddAttributesCommand eventContextAddSpanAttributesCommand;

  private EventContextAddAttributeCommand eventContextAddSpanAttributeCommand;

  private EventContextSetCurrentSpanNameCommand eventContextSetCurrentSpanNameCommand;

  private EventContextGetDistributedTraceContextMapCommand eventContextGetDistributedTraceContextMap;


  @Override
  public Optional<InternalSpan> startComponentSpan(CoreEvent coreEvent, InitialSpanInfo spanCustomizationInfo) {
    return startComponentSpan(coreEvent, spanCustomizationInfo, SUCCESSFUL_ASSERTION);
  }

  @Override
  public Optional<InternalSpan> startComponentSpan(CoreEvent coreEvent, InitialSpanInfo initialSpanInfo,
                                                   Assertion assertion) {
    return startCommand.execute(coreEvent.getContext(), enrichInitialSpanInfo(initialSpanInfo, coreEvent), assertion);
  }

  @Override
  public void endCurrentSpan(CoreEvent coreEvent) {
    endCurrentSpan(coreEvent, SUCCESSFUL_ASSERTION);
  }

  @Override
  public void endCurrentSpan(CoreEvent coreEvent, Assertion condition) {
    endCommand.execute(coreEvent.getContext(), condition);
  }

  @Override
  public void injectDistributedTraceContext(EventContext eventContext,
                                            DistributedTraceContextGetter distributedTraceContextGetter) {
    if (!distributedTraceContextGetter.isEmptyDistributedTraceContext()) {
      injectDistributedTraceContextCommand.execute(eventContext, distributedTraceContextGetter);
    }
  }

  @Override
  public void recordErrorAtCurrentSpan(CoreEvent coreEvent, Supplier<Error> errorSupplier, boolean isErrorEscapingCurrentSpan) {
    recordErrorAtCurrentSpanCommand.execute(coreEvent, errorSupplier, isErrorEscapingCurrentSpan);
  }

  @Override
  public void setCurrentSpanName(CoreEvent coreEvent, String name) {
    eventContextSetCurrentSpanNameCommand.execute(coreEvent.getContext(), name);
  }

  @Override
  public void addCurrentSpanAttribute(CoreEvent coreEvent, String key, String value) {
    eventContextAddSpanAttributeCommand.execute(coreEvent.getContext(), key, value);

  }

  @Override
  public void addCurrentSpanAttributes(CoreEvent coreEvent, Map<String, String> attributes) {
    eventContextAddSpanAttributesCommand.execute(coreEvent.getContext(), attributes);
  }

  @Override
  public SpanSnifferManager getSpanSnifferManager() {
    return eventSpanFactory.getSpanSnifferManager();
  }

  @Override
  public Map<String, String> getDistributedTraceContextMap(CoreEvent event) {
    return eventContextGetDistributedTraceContextMap.execute(event.getContext());
  }

  @Override
  public void initialise() throws InitialisationException {
    boolean enablePutTraceIdAndSpanIdInMdc = featureFlaggingService.isEnabled(PUT_TRACE_ID_AND_SPAN_ID_IN_MDC);
    startCommand = getEventContextStartSpanCommandFrom(LOGGER, ERROR_ON_EXECUTING_CORE_EVENT_TRACER_START_COMMAND_MESSAGE,
                                                       propagateTracingExceptions, eventSpanFactory,
                                                       enablePutTraceIdAndSpanIdInMdc);
    endCommand = getEventContextEndSpanCommandFrom(LOGGER, ERROR_ON_EXECUTING_CORE_EVENT_TRACER_END_COMMAND_MESSAGE,
                                                   propagateTracingExceptions, enablePutTraceIdAndSpanIdInMdc);
    injectDistributedTraceContextCommand = getEventContextInjectDistributedTraceContextCommand(LOGGER,
                                                                                               ERROR_ON_EXECUTING_CORE_EVENT_TRACER_INJECT_DISTRIBUTED_TRACE_CONTEXT_COMMAND_MESSAGE,
                                                                                               false);
    recordErrorAtCurrentSpanCommand = getEventContextRecordErrorCommand(LOGGER,
                                                                        ERROR_ON_EXECUTING_CORE_EVENT_TRACER_RECORD_ERROR_COMMAND_MESSAGE,
                                                                        propagateTracingExceptions);
    eventContextAddSpanAttributesCommand = getEventContextAddAttributesCommand(LOGGER,
                                                                               ERROR_ON_EXECUTING_CORE_EVENT_TRACER_ADD_ATTRIBUTES_COMMAND_MESSAGE,
                                                                               propagateTracingExceptions);
    eventContextAddSpanAttributeCommand = getEventContextAddAttributeCommand(LOGGER,
                                                                             ERROR_ON_EXECUTING_CORE_EVENT_TRACER_ADD_ATTRIBUTE_COMMAND_MESSAGE,
                                                                             propagateTracingExceptions);
    eventContextSetCurrentSpanNameCommand = getEventContextSetCurrentSpanNameCommand(LOGGER,
                                                                                     ERROR_ON_EXECUTING_CORE_EVENT_SET_CURRENT_SPAN_COMMAND_MESSAGE,
                                                                                     propagateTracingExceptions);
    eventContextGetDistributedTraceContextMap = getEventContextGetDistributedTraceContextMapCommand(LOGGER,
                                                                                                    ERROR_ON_EXECUTING_CORE_EVENT_GET_DISTRIBUTED_CONTEXT_SPAN_COMMAND_MESSAGE,
                                                                                                    propagateTracingExceptions);
  }
}
