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

public class FixedNameComponentCoreSpanCustomizationInfoProvider extends ComponentCoreSpanCustomizationInfoProvider {

  private final String name;

  public FixedNameComponentCoreSpanCustomizationInfoProvider(Component component, String name) {
    super(component);
    this.name = name;
  }

  @Override
  public StartSpanInfo get(CoreEvent coreEvent) {
    return new FixedComponentCoreStartSpanInfoProvider(component, coreEvent, name);
  }

  private class FixedComponentCoreStartSpanInfoProvider extends ComponentStartSpanInfo {

    private final String name;

    public FixedComponentCoreStartSpanInfoProvider(Component component, CoreEvent coreEvent, String name) {
      super(component, coreEvent);
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }
  }
}
