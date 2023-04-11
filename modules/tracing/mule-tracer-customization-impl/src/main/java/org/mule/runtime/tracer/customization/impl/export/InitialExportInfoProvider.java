/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.customization.impl.export;

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
}
