/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.privileged.profiling.tracing;

import static java.util.Collections.emptyMap;

import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.NamedSpanBasedOnParentSpanChildCustomizerSpanCustomizer;

import java.util.Map;

/**
 * A Customizer for the creation of {@link InternalSpan}.
 *
 * @since 4.5.0
 */
public interface SpanCustomizer {

  /**
   * Gets the {@link InternalSpan} name from the {@param coreEvent} and the {@param component}
   *
   * @param coreEvent the {@link CoreEvent} to resolve the span name from.
   * @return the name of the span.
   */
  String getName(CoreEvent coreEvent);

  /**
   * @param coreEvent         the {@link CoreEvent} corresponding to span being created.
   * @param muleConfiguration the {@link MuleConfiguration} corresponding to span being created.
   * @param artifactType      the {@link ArtifactType} corresponding to span being created.
   * @return the attributes of the span being created.
   */
  default Map<String, String> getAttributes(CoreEvent coreEvent, MuleConfiguration muleConfiguration,
                                            ArtifactType artifactType) {
    return emptyMap();
  }

  /**
   * Defines the {@link ChildSpanInfo} that will be set for the created Span. This customizer will be available for the child
   * spans in case it is needed.
   *
   * @see {@link NamedSpanBasedOnParentSpanChildCustomizerSpanCustomizer}
   *
   * @return the {@link ChildSpanInfo} corresponding to the span that will be created.
   */
  ChildSpanInfo getChildSpanCustomizer();

}
