/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.customization.impl.export;

import static org.mule.runtime.tracer.customization.impl.info.SpanInitialInfoUtils.getSpanName;

import static org.apache.commons.lang3.StringUtils.stripToEmpty;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.customization.api.InitialExportInfoProvider;
import org.mule.runtime.tracer.customization.api.InitialSpanInfoProvider;

import java.util.Map;

/**
 * An abstract {@link InitialExportInfoProvider} for resolving the {@link InitialExportInfo}.
 *
 * @since 4.5.0
 */
public abstract class AbstractInitialExportInfoProvider implements InitialExportInfoProvider {

  @Override
  public InitialExportInfo getInitialExportInfo(Component component) {
    return getInitialExportInfo(component, "");
  }

  @Override
  public InitialExportInfo getInitialExportInfo(Component component, String spanNameSuffix) {
    return doGetInitialExportInfo(getSpanName(component.getIdentifier()) + stripToEmpty(spanNameSuffix));
  }

  @Override
  public InitialExportInfo getInitialExportInfo(String spanName) {
    return getInitialExportInfo(spanName, false);
  }

  @Override
  public InitialExportInfo getInitialExportInfo(String spanName, boolean debugLevel) {
    if (debugLevel) {
      return doGetInitialExportInfoForDebugLevel();
    }

    return doGetInitialExportInfo(spanName);
  }

  protected abstract InitialExportInfo doGetInitialExportInfoForDebugLevel();

  private InitialExportInfo doGetInitialExportInfo(String componentFullyQualifiedName) {
    InitialExportInfo initialExportInfo = getInitialExportInfoMapByName().get(componentFullyQualifiedName);

    if (initialExportInfo == null) {
      initialExportInfo = getDefaultInitialExportInfo();
    }

    return initialExportInfo;
  }

  /**
   * @return the default initial export info for this {@link InitialSpanInfoProvider}
   */
  protected abstract InitialExportInfo getDefaultInitialExportInfo();

  /**
   * @return a map that has named {@link InitialExportInfo}
   */
  protected abstract Map<String, InitialExportInfo> getInitialExportInfoMapByName();

}
