/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.tracing.customization;

import org.mule.runtime.api.component.Component;

public class FixedComponentCoreExecutionInitialSpanInfo extends ComponentExecutionInitialSpanInfo {

  private final String name;

  public FixedComponentCoreExecutionInitialSpanInfo(Component component, String name) {
    super(component);
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }
}
