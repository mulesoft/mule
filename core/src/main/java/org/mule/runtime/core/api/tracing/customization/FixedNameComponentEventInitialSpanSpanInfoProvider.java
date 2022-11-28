/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.tracing.customization;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;

/**
 * A {@link ComponentEventBasedInitialSpanInfoProvider} provider for {@link InitialSpanInfo} with fixed name.
 *
 * @since 4.5.0
 */
public class FixedNameComponentEventInitialSpanSpanInfoProvider
    extends ComponentEventBasedInitialSpanInfoProvider {

  private final String name;

  public FixedNameComponentEventInitialSpanSpanInfoProvider(Component component, String name) {
    super(component);
    this.name = name;
  }

  @Override
  public InitialSpanInfo get(CoreEvent coreEvent) {
    return new FixedComponentCoreExecutionInitialSpanInfoProvider(component, coreEvent, name);
  }

  private class FixedComponentCoreExecutionInitialSpanInfoProvider extends ComponentExecutionInitialSpanInfo {

    private final String name;

    public FixedComponentCoreExecutionInitialSpanInfoProvider(Component component, CoreEvent coreEvent, String name) {
      super(component, coreEvent);
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }
  }
}
