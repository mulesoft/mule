/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.tracing.customization;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.tracer.api.span.info.StartSpanInfo;

/**
 * A {@link EventBasedStartSpanCustomizationInfoProvider} for {@link StartSpanInfo} based on a {@link Component}.
 *
 * @since 4.5.0
 */
public class ComponentExecutionBasedStartSpanCustomizationInfoProvider implements EventBasedStartSpanCustomizationInfoProvider {

  protected final Component component;

  public ComponentExecutionBasedStartSpanCustomizationInfoProvider(Component component) {
    this.component = component;
  }

  @Override
  public StartSpanInfo get(CoreEvent coreEvent) {
    return new ComponentExecutionStartSpanInfo(component, coreEvent);
  }
}
