/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.exporter.impl.optel.sdk;

import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;

import static io.opentelemetry.sdk.common.InstrumentationLibraryInfo.create;
import static io.opentelemetry.sdk.internal.InstrumentationScopeUtil.toInstrumentationScopeInfo;

/**
 * Constants for the instrumentation logic that provides information required by the OTEL sdk.
 *
 * @since 4.5.0
 */
public class OpenTelemetryInstrumentationConstants {

  public static final String MULE_INSTRUMENTATION_LIBRARY = "mule";
  public static final String MULE_INSTRUMENTATION_LIBRARY_VERSION = "1.0.0";
  public static final InstrumentationLibraryInfo INSTRUMENTATION_LIBRARY_INFO =
      create(MULE_INSTRUMENTATION_LIBRARY, MULE_INSTRUMENTATION_LIBRARY_VERSION);
  public static final InstrumentationScopeInfo INSTRUMENTATION_SCOPE_INFO =
      toInstrumentationScopeInfo(INSTRUMENTATION_LIBRARY_INFO);
}
