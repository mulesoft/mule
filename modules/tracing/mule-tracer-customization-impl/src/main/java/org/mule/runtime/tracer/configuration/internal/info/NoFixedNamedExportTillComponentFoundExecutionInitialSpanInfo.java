/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.configuration.internal.info;

import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;

/**
 * A {@link InitialSpanInfo} with a fixedName that indicates that a hierarchy of spans shouldn't be exported till execute-next
 * component.
 *
 * @since 4.6.0
 */
public class NoFixedNamedExportTillComponentFoundExecutionInitialSpanInfo implements InitialSpanInfo {

  private final String spanName;
  private final String forceNotExportUntilComponentName;

  public NoFixedNamedExportTillComponentFoundExecutionInitialSpanInfo(String spanName, String forceNotExportUntilComponentName) {
    this.spanName = spanName;
    this.forceNotExportUntilComponentName = forceNotExportUntilComponentName;
  }

  @Override
  public String getName() {
    return spanName;
  }

  @Override
  public InitialExportInfo getInitialExportInfo() {
    return new NoExportTillSpanWithNameInitialExportInfo(forceNotExportUntilComponentName, true);
  }
}
