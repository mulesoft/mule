/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.customization.impl.export;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.customization.api.InitialExportInfoProvider;

import java.util.Set;

public class TracingLevelExportInfo {

  private InitialExportInfoProvider initialExportInfoProvider;
  private Object spanIdentifier;
  private boolean isOverride;

  public TracingLevelExportInfo(InitialExportInfoProvider initialExportInfoProvider, boolean isOverride) {
    this.initialExportInfoProvider = initialExportInfoProvider;
    this.isOverride = isOverride;
  }

  public void setSpanIdentifier(Component spanIdentifier) {
    this.spanIdentifier = spanIdentifier;
  }

  public void setSpanIdentifier(String spanIdentifier) {
    this.spanIdentifier = spanIdentifier;
  }

  public boolean isOverride() {
    return this.isOverride;
  }

  public boolean isExportable() {
    return getInitialExportInfo().isExportable();
  }

  public Set<String> noExportUntil() {
    return getInitialExportInfo().noExportUntil();
  }

  private InitialExportInfo getInitialExportInfo() {
    if (this.spanIdentifier instanceof Component) {
      return this.initialExportInfoProvider.getInitialExportInfo((Component) spanIdentifier);
    }
    return this.initialExportInfoProvider.getInitialExportInfo((String) spanIdentifier);
  }

  public void propagateExportInfo(TracingLevelExportInfo parentTracingLevelExportInfo) {
    if (!isOverride() && parentTracingLevelExportInfo.isOverride()) {
      this.initialExportInfoProvider = parentTracingLevelExportInfo.getInitialExportInfoProvider();
      this.isOverride = true;
    }
  }

  private InitialExportInfoProvider getInitialExportInfoProvider() {
    return this.initialExportInfoProvider;
  }

}
