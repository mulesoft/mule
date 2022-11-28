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
 * A {@link ComponentExecutionBasedStartSpanCustomizationInfoProvider} provider for {@link StartSpanInfo} with fixed name.
 *
 * @since 4.5.0
 */
public class FixedNameComponentExecutionStartSpanInfoProvider
    extends ComponentExecutionBasedStartSpanCustomizationInfoProvider {

  private final String name;

  public FixedNameComponentExecutionStartSpanInfoProvider(Component component, String name) {
    super(component);
    this.name = name;
  }

  @Override
  public StartSpanInfo get(CoreEvent coreEvent) {
    return new FixedComponentCoreExecutionStartSpanInfoProvider(component, coreEvent, name);
  }

  private class FixedComponentCoreExecutionStartSpanInfoProvider extends ComponentExecutionStartSpanInfo {

    private final String name;

    public FixedComponentCoreExecutionStartSpanInfoProvider(Component component, CoreEvent coreEvent, String name) {
      super(component, coreEvent);
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }
  }
}
