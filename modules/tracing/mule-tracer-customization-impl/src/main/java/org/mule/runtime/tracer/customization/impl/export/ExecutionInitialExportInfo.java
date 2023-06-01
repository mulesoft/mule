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

public class ExecutionInitialExportInfo implements InitialExportInfo {

  private InitialExportInfoProvider initialExportInfoProvider;
  private boolean exportable;
  private Set<String> resetSpans;
  private final Object spanIdentifier;

  public ExecutionInitialExportInfo(InitialExportInfoProvider initialExportInfoProvider, Component component) {
    this.initialExportInfoProvider = initialExportInfoProvider;
    this.exportable = initialExportInfoProvider.getInitialExportInfo(component).isExportable();
    this.resetSpans = initialExportInfoProvider.getInitialExportInfo(component).noExportUntil();
    this.spanIdentifier = component;
  }

  public ExecutionInitialExportInfo(InitialExportInfoProvider initialExportInfoProvider, String name) {
    this.initialExportInfoProvider = initialExportInfoProvider;
    this.exportable = initialExportInfoProvider.getInitialExportInfo(name).isExportable();
    this.resetSpans = initialExportInfoProvider.getInitialExportInfo(name).noExportUntil();
    this.spanIdentifier = name;
  }

  @Override
  public boolean isExportable() {
    return this.exportable;
  }

  @Override
  public Set<String> noExportUntil() {
    return resetSpans;
  }

  @Override
  public void propagateInitialExportInfo(InitialExportInfo parentInitialExportInfo) {
    InitialExportInfoProvider parentInitialExportInfoProvider =
        ((ExecutionInitialExportInfo) parentInitialExportInfo).getInitialExportInfoProvider();

    if (parentInitialExportInfoProvider.isOverride() && !this.initialExportInfoProvider.isOverride()) {
      this.initialExportInfoProvider = parentInitialExportInfoProvider;

      if (this.spanIdentifier instanceof Component) {
        this.exportable = parentInitialExportInfoProvider.getInitialExportInfo(((Component) this.spanIdentifier)).isExportable();
        this.resetSpans = parentInitialExportInfoProvider.getInitialExportInfo(((Component) this.spanIdentifier)).noExportUntil();
      } else {
        this.exportable = parentInitialExportInfoProvider.getInitialExportInfo(((String) this.spanIdentifier)).isExportable();
        this.resetSpans = parentInitialExportInfoProvider.getInitialExportInfo(((String) this.spanIdentifier)).noExportUntil();
      }
    }
  }

  public InitialExportInfoProvider getInitialExportInfoProvider() {
    return this.initialExportInfoProvider;
  }

}
