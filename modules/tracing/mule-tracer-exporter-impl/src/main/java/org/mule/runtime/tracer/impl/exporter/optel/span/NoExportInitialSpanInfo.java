/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter.optel.span;

import org.mule.runtime.core.api.tracing.customization.NoExportTillSpanWithNameInitialExportInfo;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;

import java.util.Set;

public class NoExportInitialSpanInfo implements InitialSpanInfo {

  private final NoExportTillSpanWithNameInitialExportInfo initialExportInfo;

  public NoExportInitialSpanInfo(Set<String> noExportUntil) {
    this.initialExportInfo = new NoExportTillSpanWithNameInitialExportInfo(noExportUntil, false);;
  }

  @Override
  public String getName() {
    return null;
  }


  @Override
  public InitialExportInfo getInitialExportInfo() {
    return initialExportInfo;
  }

  @Override
  public int getInitialAttributesCount() {
    return 0;
  }
}
