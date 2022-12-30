/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.tracing.customization;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;

/**
 * A {@link ComponentExecutionInitialSpanInfo} that indicates that a hierarchy of spans shouldn't be exported till execute-next
 * component.
 *
 * @since 4.5.0
 */
public class NoExportTillExecuteNextComponentExecutionInitialSpanInfo extends ComponentExecutionInitialSpanInfo {

  public static final String EXECUTE_NEXT = "execute-next";

  public NoExportTillExecuteNextComponentExecutionInitialSpanInfo(Component component) {
    super(component);
  }

  @Override
  public InitialExportInfo getInitialExportInfo() {
    return new NoExportTillSpanWithNameInitialExportInfo(EXECUTE_NEXT, true);
  }

  @Override
  public boolean isPolicySpan() {
    return true;
  }
}
