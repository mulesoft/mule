/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import org.mule.runtime.tracer.api.span.InternalSpan;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public interface MuleOpenTelemetrySpan extends Span {

  Context getSpanOpenTelemetryContext();

  void end(InternalSpan internalSpan);

  Map<String, String> getDistributedTraceContextMap();

  void setNoExportUntil(Set<String> noExportableUntil);

  default Set<String> getNoExportUntil() {
    return Collections.emptySet();
  }

  void setNotIntercepting(boolean propagateUpdateName);

  void setCustomizableInformationCarrier(boolean propagateSpanFromParent);

  boolean isNotIntercepting();

  boolean isSetCustomizableInformationCarrier();
}
