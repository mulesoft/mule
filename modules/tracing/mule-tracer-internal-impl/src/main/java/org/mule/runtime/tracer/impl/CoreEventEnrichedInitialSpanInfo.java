/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.tracer.api.span.info.EnrichedInitialSpanInfo;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class CoreEventEnrichedInitialSpanInfo implements EnrichedInitialSpanInfo {

  private static final String CORRELATION_ID_KEY = "correlation.id";
  private static final String THREAD_START_NAME_KEY = "thread.start.name";
  private static final String THREAD_START_ID_KEY = "thread.start.id";
  public static final int ADDITIONAL_SPAN_ATTRIBUTES_COUNT = 3;

  private final InitialSpanInfo baseInitialSpanInfo;
  private final String correlationId;
  private final String threadStartName;
  private final String threadStartId;
  private final Map<String, String> tracingVariables = new HashMap<>();

  public CoreEventEnrichedInitialSpanInfo(InitialSpanInfo baseInitialSpanInfo, CoreEvent coreEvent) {
    this.baseInitialSpanInfo = baseInitialSpanInfo;
    this.correlationId = coreEvent.getCorrelationId();
    this.threadStartName = Thread.currentThread().getName();
    this.threadStartId = Long.toString(Thread.currentThread().getId());
    if (coreEvent instanceof PrivilegedEvent) {
      ((PrivilegedEvent) coreEvent).getLoggingVariables().ifPresent(tracingVariables::putAll);
    }
  }

  @Override
  public String getName() {
    return baseInitialSpanInfo.getName();
  }

  @Override
  public InitialExportInfo getInitialExportInfo() {
    return baseInitialSpanInfo.getInitialExportInfo();
  }

  @Override
  public void forEachAttribute(BiConsumer<String, String> biConsumer) {
    baseInitialSpanInfo.forEachAttribute(biConsumer);
    biConsumer.accept(CORRELATION_ID_KEY, correlationId);
    biConsumer.accept(THREAD_START_NAME_KEY, threadStartName);
    biConsumer.accept(THREAD_START_ID_KEY, threadStartId);
    tracingVariables.forEach(biConsumer);
  }

  @Override
  public boolean isRootSpan() {
    return baseInitialSpanInfo.isRootSpan();
  }

  @Override
  public boolean isPolicySpan() {
    return baseInitialSpanInfo.isPolicySpan();
  }

  @Override
  public int getInitialAttributesCount() {
    return baseInitialSpanInfo.getInitialAttributesCount() + ADDITIONAL_SPAN_ATTRIBUTES_COUNT + tracingVariables.size();
  }

  @Override
  public InitialSpanInfo getBaseInitialSpanInfo() {
    return baseInitialSpanInfo;
  }
}
