/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.privileged.profiling.tracing;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.emptyMap;

import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.NamedSpanBasedOnParentSpanChildSpanCustomizationInfo;

import java.util.Map;

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
   * Indicates the level until which the span hierarchy will be exported. For example: if the level is 2, the grandchildren of
   * this span will not be exported.
   *
   * This can be overridden.
   *
   * @see #ignoreExportLevelLimitOfAncestors()
   *
   * @return the level until which the child span hierarchy will be exported.
   */
  default int exportUntilLevel() {
    return MAX_VALUE;
  }

  /**
   * Indicates that both this span and the hierarchy of children will be exported ignoring if one of the ancestors has set the
   * export until a certain level.
   *
   * @see #exportUntilLevel()
   *
   * @return if it forces the export ignoring the previous limits set by parents.
   */
  default boolean ignoreExportLevelLimitOfAncestors() {
    return false;
  }

}
