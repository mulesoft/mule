/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.tracing.customization;

import static org.mule.runtime.tracer.api.span.info.StartExportInfo.NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;

import org.mule.runtime.tracer.api.span.info.StartExportInfo;

/**
 * A {@link FixedNameStartSpanInfo} that indicates that the span to be created is not exportable.
 *
 * @since 4.5.0
 */
public class NoExportFixedNameStartSpanInfo extends FixedNameStartSpanInfo {

  public NoExportFixedNameStartSpanInfo(String name) {
    super(name);
  }

  @Override
  public StartExportInfo getStartExportInfo() {
    return NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;
  }
}
