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

public class ExecutionInitialExportInfo implements InitialExportInfo {

  private InitialExportInfoProvider initialExportInfoProvider;
  private boolean exportable;
  private final Object spanIdentifier;

  public ExecutionInitialExportInfo(InitialExportInfoProvider initialExportInfoProvider, Component component) {
    this.initialExportInfoProvider = initialExportInfoProvider;
    this.exportable = initialExportInfoProvider.getInitialExportInfo(component).isExportable();
    this.spanIdentifier = component;
  }

  public ExecutionInitialExportInfo(InitialExportInfoProvider initialExportInfoProvider, String name) {
    this.initialExportInfoProvider = initialExportInfoProvider;
    this.exportable = initialExportInfoProvider.getInitialExportInfo(name).isExportable();
    this.spanIdentifier = name;
  }

  @Override
  public boolean isExportable() {
    return exportable;
  }

  @Override
  public void propagateInitialExportInfo(InitialExportInfo parentInitialExportInfo) {
    InitialExportInfoProvider parentInitialExportInfoProvider =
        ((ExecutionInitialExportInfo) parentInitialExportInfo).getInitialExportInfoProvider();

    if (parentInitialExportInfoProvider.isOverride() && !this.initialExportInfoProvider.isOverride()) {
      this.initialExportInfoProvider = parentInitialExportInfoProvider;

      if (this.spanIdentifier instanceof Component) {
        this.exportable = this.initialExportInfoProvider.getInitialExportInfo(((Component) this.spanIdentifier)).isExportable();
      } else {
        this.exportable = this.initialExportInfoProvider.getInitialExportInfo(((String) this.spanIdentifier)).isExportable();
      }
    }
  }

  public InitialExportInfoProvider getInitialExportInfoProvider() {
    return this.initialExportInfoProvider;
  }

}
