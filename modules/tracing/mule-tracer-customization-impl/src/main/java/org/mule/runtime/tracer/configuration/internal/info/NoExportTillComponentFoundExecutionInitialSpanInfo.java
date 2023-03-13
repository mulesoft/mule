/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.configuration.internal.info;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;

/**
 * A {@link ComponentExecutionInitialSpanInfo} that indicates that a hierarchy of spans shouldn't be exported till execute-next
 * component.
 *
 * @since 4.5.0
 */
public class NoExportTillComponentFoundExecutionInitialSpanInfo extends ComponentExecutionInitialSpanInfo {

  private final String forceNotExportUntilComponentName;

  public NoExportTillComponentFoundExecutionInitialSpanInfo(Component component, String forceNotExportUntilComponentName) {
    super(component);
    this.forceNotExportUntilComponentName = forceNotExportUntilComponentName;
  }

  @Override
  public InitialExportInfo getInitialExportInfo() {
    return new NoExportTillSpanWithNameInitialExportInfo(forceNotExportUntilComponentName, true);
  }

  @Override
  public boolean isPolicySpan() {
    return true;
  }
}
