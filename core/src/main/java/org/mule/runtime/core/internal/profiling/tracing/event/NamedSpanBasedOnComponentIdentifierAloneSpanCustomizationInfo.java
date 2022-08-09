/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event;

import static org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanUtils.getSpanName;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.tracing.event.span.AbstractNamedSpanBasedOnComponentIdentifierSpanCustomizationInfo;
import org.mule.runtime.core.privileged.profiling.tracing.SpanCustomizationInfo;

/**
 * A {@link SpanCustomizationInfo} that sets the span name based on the {@link org.mule.runtime.api.component.ComponentIdentifier}
 * of the {@link Component} that corresponds to that span. It doesn't take into account any other detail
 *
 * @since 4.5.0
 */
public class NamedSpanBasedOnComponentIdentifierAloneSpanCustomizationInfo
    extends AbstractNamedSpanBasedOnComponentIdentifierSpanCustomizationInfo {

  public NamedSpanBasedOnComponentIdentifierAloneSpanCustomizationInfo(Component component) {
    super(component);
  }

  @Override
  public String getName(CoreEvent coreEvent) {
    return getSpanName(component.getIdentifier());
  }
}
