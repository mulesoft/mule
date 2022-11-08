/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.privileged.profiling.tracing;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.NamedSpanBasedOnParentSpanChildSpanCustomizationInfo;

import java.util.Map;
import java.util.Set;

/**
 * Info for a customized creation of an {@link InternalSpan}.
 *
 * @since 4.5.0
 */
public interface SpanCustomizationInfo {

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
   * Defines the {@link ChildSpanCustomizationInfo} that will be set for the created Span. This customization info will be
   * available for the child spans in case it is needed.
   *
   * @see {@link NamedSpanBasedOnParentSpanChildSpanCustomizationInfo}
   *
   * @return the {@link ChildSpanCustomizationInfo} corresponding to the span that will be created.
   */
  ChildSpanCustomizationInfo getChildSpanCustomizationInfo();

  /**
   * @return if the span should be exported.
   */
  default boolean isExportable(CoreEvent coreEvent) {
    return true;
  }

  /**
   * Indicates that no children will be exported till a span is found with the names returned.
   *
   * For example: in case noExportUntil returns "execute-next", no children will be exported till an execute-next span.
   *
   * ------------- span (exported) --------------------------------------------------------- |___ logger (not exported) ____ |___
   * scope (not exported) |___ execute-next (exported) |__ flow (exported)
   *
   * @return the name of the spans where the span hierarchy is exported again.
   */
  default Set<String> noExportUntil() {
    return emptySet();
  }

  /**
   * @return whether this corresponds to a policy span.
   */
  default boolean isPolicySpan() {
    return false;
  }
}
