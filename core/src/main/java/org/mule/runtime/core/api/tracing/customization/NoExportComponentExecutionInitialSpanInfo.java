/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.tracing.customization;

import static org.mule.runtime.tracer.api.span.info.InitialExportInfo.NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;

/**
 * A {@link ComponentExecutionInitialSpanInfo} that indicates that the span shouldn't be exportable.
 *
 * @since 4.5.0
 */
public class NoExportComponentExecutionInitialSpanInfo extends ComponentExecutionInitialSpanInfo {

  public NoExportComponentExecutionInitialSpanInfo(Component component) {
    super(component);
  }

  @Override
  public InitialExportInfo getInitialExportInfo() {
    return NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;
  }
}
