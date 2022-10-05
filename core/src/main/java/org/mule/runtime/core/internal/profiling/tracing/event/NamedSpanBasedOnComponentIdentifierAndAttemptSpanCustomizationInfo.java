/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.tracing.event;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.profiling.tracing.SpanCustomizationInfo;

/**
 * A {@link SpanCustomizationInfo} that sets the span name based on the {@link org.mule.runtime.api.component.ComponentIdentifier}
 * of the {@link Component} that belongs to the current span and adds a suffix which indicates it represents an attempt and, if it
 * corresponds, the number of the attempt
 *
 * @since 4.5.0
 */
public class NamedSpanBasedOnComponentIdentifierAndAttemptSpanCustomizationInfo
    extends NamedSpanBasedOnComponentIdentifierAloneSpanCustomizationInfo {

  private Long attemptNumber;

  public NamedSpanBasedOnComponentIdentifierAndAttemptSpanCustomizationInfo(Component component) {
    super(component);
  }

  public NamedSpanBasedOnComponentIdentifierAndAttemptSpanCustomizationInfo(Component component, Long attemptNumber) {
    super(component);
    this.attemptNumber = attemptNumber;
  }

  @Override
  public String getName(CoreEvent coreEvent) {
    return super.getName(coreEvent) + ":attempt" + (attemptNumber != null ? ":" + attemptNumber : "");
  }
}
