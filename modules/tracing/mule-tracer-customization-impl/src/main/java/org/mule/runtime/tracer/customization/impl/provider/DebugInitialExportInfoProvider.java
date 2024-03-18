/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.customization.impl.provider;

import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.customization.api.InternalSpanNames;
import org.mule.runtime.tracer.customization.api.InitialExportInfoProvider;
import org.mule.runtime.tracer.customization.impl.export.NoExportTillSpanWithNameInitialExportInfo;
import org.mule.runtime.tracing.level.api.config.TracingLevel;

import java.util.HashMap;
import java.util.Map;

import static org.mule.runtime.tracer.api.span.info.InitialExportInfo.DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;
import static org.mule.runtime.tracer.api.span.info.InitialExportInfo.NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.ASYNC_INNER_CHAIN_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.CACHE_CHAIN_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.GET_CONNECTION_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.OPERATION_EXECUTION_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.MESSAGE_PROCESSORS_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.PARAMETERS_RESOLUTION_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.POLICY_CHAIN_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.POLICY_NEXT_ACTION_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.TRY_SCOPE_INNER_CHAIN_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.VALUE_RESOLUTION_SPAN_NAME;
import static org.mule.runtime.tracer.customization.impl.info.SpanInitialInfoUtils.UNKNOWN;


/**
 * An {@link InitialExportInfoProvider} corresponding to the {@link TracingLevel#DEBUG}
 *
 * @since 4.5.0
 */
public class DebugInitialExportInfoProvider extends MonitoringInitialExportInfoProvider {

  private final Map<String, InitialExportInfo> initialExportInfoMapByName = new HashMap<String, InitialExportInfo>() {

    {
      put(POLICY_CHAIN_SPAN_NAME,
          new NoExportTillSpanWithNameInitialExportInfo(InternalSpanNames.EXECUTE_NEXT_SPAN_NAME, true));
      put(POLICY_NEXT_ACTION_SPAN_NAME, NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO);
      put(UNKNOWN, NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO);
      put(ASYNC_INNER_CHAIN_SPAN_NAME, NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO);
      put(TRY_SCOPE_INNER_CHAIN_SPAN_NAME, NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO);
      put(CACHE_CHAIN_SPAN_NAME, NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO);
      put(MESSAGE_PROCESSORS_SPAN_NAME, NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO);
      put(GET_CONNECTION_SPAN_NAME, DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO);
      put(PARAMETERS_RESOLUTION_SPAN_NAME, DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO);
      put(VALUE_RESOLUTION_SPAN_NAME, DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO);
      put(OPERATION_EXECUTION_SPAN_NAME, DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO);
    }
  };

  @Override
  protected InitialExportInfo doGetInitialExportInfoForDebugLevel() {
    return DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;
  }

  protected Map<String, InitialExportInfo> getInitialExportInfoMapByName() {
    return initialExportInfoMapByName;
  }
}
