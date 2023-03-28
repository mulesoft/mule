/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.configuration.internal.provider;

import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.configuration.internal.export.InitialExportInfoProvider;
import org.mule.runtime.tracing.level.api.config.TracingLevel;

import static org.mule.runtime.tracer.api.span.info.InitialExportInfo.DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;


/**
 * An {@link InitialExportInfoProvider} corresponding to the {@link TracingLevel#DEBUG}
 *
 * @since 4.6.0
 */
public class DebugInitialExportInfoProvider extends MonitoringInitialExportInfoProvider {

  @Override
  protected InitialExportInfo doGetInitialExportInfoForDebugLevel() {
    return DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;
  }
}
