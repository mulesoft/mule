/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.tracing.event;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.event.CoreEvent;

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
