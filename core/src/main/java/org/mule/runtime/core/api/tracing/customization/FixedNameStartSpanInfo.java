/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.tracing.customization;

import org.mule.runtime.tracer.api.span.info.StartExportInfo;
import org.mule.runtime.tracer.api.span.info.StartSpanInfo;

/**
 * A {@link StartSpanInfo} with a fix name.
 *
 * @since 4.5.0
 */
public class FixedNameStartSpanInfo implements StartSpanInfo {

  private final String name;

  public FixedNameStartSpanInfo(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }


  @Override
  public StartExportInfo getStartExportInfo() {
    return StartExportInfo.DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;
  }
}
