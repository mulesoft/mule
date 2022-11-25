/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.tracing.customization;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.tracer.api.span.info.StartExportInfo;

public class NoExportableComponentSpanCustomizationInfo extends ComponentStartSpanInfo {

  public NoExportableComponentSpanCustomizationInfo(Component component, CoreEvent coreEvent) {
    super(component, coreEvent);
  }

  @Override
  public StartExportInfo getStartExportInfo() {
    return StartExportInfo.NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;
  }
}
