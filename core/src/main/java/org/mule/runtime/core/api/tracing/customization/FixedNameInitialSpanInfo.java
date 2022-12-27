/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.tracing.customization;

import static org.mule.runtime.tracer.api.span.info.InitialExportInfo.DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;

import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;

/**
 * A {@link InitialSpanInfo} with a fixed name.
 *
 * @since 4.5.0
 */
public class FixedNameInitialSpanInfo implements InitialSpanInfo {

  private final String name;

  public FixedNameInitialSpanInfo(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }


  @Override
  public InitialExportInfo getInitialExportInfo() {
    return DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;
  }

  @Override
  public int getInitialAttributesCount() {
    return 0;
  }
}
