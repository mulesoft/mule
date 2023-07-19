/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.customization.api;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;

public interface InitialExportInfoProvider {

  /**
   * @param component the {@link Component} the component.
   *
   * @return {@link InitialExportInfo} for a component.
   */
  InitialExportInfo getInitialExportInfo(Component component);

  /**
   * @param component      the {@link Component} the component.
   * @param spanNameSuffix the span name suffix
   *
   * @return {@link InitialExportInfo} for a component.
   */
  InitialExportInfo getInitialExportInfo(Component component, String spanNameSuffix);

  /**
   * @param spanName the span name.
   *
   * @return {@link InitialExportInfo} for a component.
   */
  InitialExportInfo getInitialExportInfo(String spanName);

  /**
   * @param spanName   the span name.
   *
   * @param debugLevel indicates if it is debug level.
   *
   * @return {@link InitialExportInfo} for a component.
   */
  InitialExportInfo getInitialExportInfo(String spanName, boolean debugLevel);
}
