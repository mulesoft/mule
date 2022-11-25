/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter;

import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.info.StartSpanInfo;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;

public interface MuleOpenTelemetrySpan extends Span {

  /**
   * The OpenTelemetry Api Context
   *
   * @return the {@Context}
   */
  Context getSpanOpenTelemetryContext();

  /**
   * Ends the span
   *
   * @param internalSpan  the internal span.
   * @param startSpanInfo the start span info.
   * @param artifactId    the artifact id.
   * @param artifactType  the artifact type.
   */
  void end(InternalSpan internalSpan, StartSpanInfo startSpanInfo, String artifactId, String artifactType);

  Map<String, String> getDistributedTraceContextMap();

  /**
   * Indicates that no children will be exported till a span is found with the names returned.
   * <p>
   * For example: in case noExportUntil returns "execute-next", no children will be exported till an execute-next span.
   * <p>
   * ------------- span (exported) --------------------------------------------------------- |___ logger (not exported) ____ |___
   * scope (not exported) |___ execute-next (exported) |__ flow (exported)
   *
   * @return the name of the spans where the span hierarchy is exported again.
   */
  void setNoExportUntil(Set<String> noExportableUntil);

  default Set<String> getNoExportUntil() {
    return Collections.emptySet();
  }

  /**
   * Whether it is a policy span.
   *
   * @param policy value to set
   */
  void setPolicy(boolean policy);

  /**
   * Whether it is a policy span.
   *
   * @param root value to set
   */
  void setRoot(boolean root);

  /**
   * @return if it should only propagate name and attributes.
   */
  boolean onlyPropagateNamesAndAttributes();

  /**
   * @return if it is a root span for a flow. For example, if it is a flow after the policies spans.
   */
  boolean isRoot();
}
