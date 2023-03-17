/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.configuration.internal.provider;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.configuration.api.InitialSpanInfoProvider;
import org.mule.runtime.tracer.configuration.internal.export.MonitoringInitialExportInfoProvider;
import org.mule.runtime.tracer.configuration.internal.export.InitialExportInfoProvider;
import org.mule.runtime.tracer.configuration.internal.info.ExecutionInitialSpanInfo;

/**
 * Default implementation of {@link InitialSpanInfoProvider}
 *
 * @since 4.6.0
 */
public class DefaultInitialSpanInfoProvider implements InitialSpanInfoProvider {

  // TODO: User Story B - Implementation of Monitoring, Troubleshooting, App Level (W-12658074)
  private final InitialExportInfoProvider initialExportInfo = new MonitoringInitialExportInfoProvider();

  @Override
  public InitialSpanInfo getInitialSpanInfoFrom(Component component) {
    return new ExecutionInitialSpanInfo(component, initialExportInfo);
  }

  @Override
  public InitialSpanInfo getInitialSpanInfoFrom(Component component, String suffix) {
    return new ExecutionInitialSpanInfo(component, initialExportInfo, null, suffix);
  }

  @Override
  public InitialSpanInfo getInitialSpanInfoFrom(String name) {
    return new ExecutionInitialSpanInfo(name, initialExportInfo);
  }

  @Override
  public InitialSpanInfo getInitialSpanInfoFrom(Component component, String overriddenName, String suffix) {
    return new ExecutionInitialSpanInfo(component, overriddenName, initialExportInfo);
  }
}
