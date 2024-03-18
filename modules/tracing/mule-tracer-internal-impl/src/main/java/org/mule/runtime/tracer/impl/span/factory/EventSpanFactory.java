/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl.span.factory;

import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.tracer.api.sniffer.SpanSnifferManager;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.impl.span.InternalSpan;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;

/**
 * Provides the Span from the core event and the component.
 *
 * @since 4.5.0
 */
public interface EventSpanFactory {

  /**
   * Provides a span for related to a component that is hit by an event.
   *
   * @param spanContext     the {@link SpanContext}.
   * @param artifactId      the artifact id.
   * @param artifactType    the {@link ArtifactType}
   * @param initialSpanInfo the {@link InitialSpanInfo}.
   *
   * @return the resulting {@link InternalSpan}
   */
  InternalSpan getSpan(SpanContext spanContext,
                       InitialSpanInfo initialSpanInfo);

  SpanSnifferManager getSpanSnifferManager();
}
