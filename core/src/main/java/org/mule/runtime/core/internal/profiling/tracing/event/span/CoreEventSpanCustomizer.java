/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.event.CoreEvent;

import java.util.Map;

import static java.util.Collections.emptyMap;

/**
 * A Customizer for the creation of {@link InternalSpan}.
 *
 * @since 4.5.0
 */
public interface CoreEventSpanCustomizer {

  /**
   * Gets the {@link InternalSpan} name from the {@param coreEvent} andthe {@param component}
   *
   * @param coreEvent the {@link CoreEvent} to resolve the span name from.
   * @param component the {@link Component} to resolve the span name from.
   * @return the name of the span.
   */
  String getName(CoreEvent coreEvent, Component component);

  /**
   * @param coreEvent         the {@link CoreEvent} corresponding to span being created.
   * @param component         the {@link Component} corresponding to span being created.
   * @param muleConfiguration the {@link MuleConfiguration} corresponding to span being created.
   * @param artifactType      the {@link ArtifactType} corresponding to span being created.
   *
   * @return the attributes of the span being created.
   */
  default Map<String, String> getAttributes(CoreEvent coreEvent, Component component, MuleConfiguration muleConfiguration,
                                            ArtifactType artifactType) {
    return emptyMap();
  }

}
