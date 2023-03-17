/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.configuration.internal;

import static org.apache.commons.lang3.StringUtils.stripToEmpty;
import static org.mule.runtime.tracer.api.span.info.InitialExportInfo.DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;
import static org.mule.runtime.tracer.api.span.info.InitialExportInfo.NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;
import static org.mule.runtime.tracer.configuration.internal.info.SpanInitialInfoUtils.UNKNOWN;
import static org.mule.runtime.tracer.configuration.internal.info.SpanInitialInfoUtils.getSpanName;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.configuration.internal.export.InitialExportInfoProvider;
import org.mule.runtime.tracer.configuration.internal.info.NoExportComponentExecutionInitialSpanInfo;
import org.mule.runtime.tracer.configuration.internal.info.NoExportTillSpanWithNameInitialExportInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * The monitoring level {@link InitialExportInfoProvider}.
 *
 * @since 4.6.0
 */
public class MonitoringInitialExportInfoProvider implements InitialExportInfoProvider {

  public static final String EXECUTE_NEXT_COMPONENT_NAME = "execute-next";
  public static final String MULE_POLICY_CHAIN_INITIAL_EXPORT_INFO_KEY = "mule:policy-chain";

  public static final String MULE_POLICY_NEXT_ACTION_EXPORT_INFO_KEY = "policy-next-action";
  public static final String MULE_POLICY_ASYNC_INNER_CHAIN_EXPORT_INFO_KEY = "async-inner-chain";
  public static final String ASYNC_INNER_CHAIN = "async-inner-chain";
  private Map<String, InitialExportInfo> initialExportInfoMap = new HashMap<String, InitialExportInfo>() {

    {
      put(MULE_POLICY_CHAIN_INITIAL_EXPORT_INFO_KEY,
          new NoExportTillSpanWithNameInitialExportInfo(EXECUTE_NEXT_COMPONENT_NAME, true));
      put(MULE_POLICY_NEXT_ACTION_EXPORT_INFO_KEY, NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO);
      put(UNKNOWN, NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO);
      put(MULE_POLICY_ASYNC_INNER_CHAIN_EXPORT_INFO_KEY, NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO);
      put(ASYNC_INNER_CHAIN, NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO);
    }
  };

  @Override
  public InitialExportInfo getInitialExportInfo(Component component) {
    return doGetInitialExportInfo(getSpanName(component.getIdentifier()));
  }

  @Override
  public InitialExportInfo getInitialExportInfo(Component component, String suffix) {
    return doGetInitialExportInfo(getSpanName(component.getIdentifier()) + stripToEmpty(suffix));
  }

  @Override
  public InitialExportInfo getInitialExportInfo(String name) {
    return doGetInitialExportInfo(name);
  }

  private InitialExportInfo doGetInitialExportInfo(String component) {
    InitialExportInfo initialExportInfo = initialExportInfoMap.get(component);

    if (initialExportInfo == null) {
      initialExportInfo = DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;
    }

    return initialExportInfo;
  }
}
