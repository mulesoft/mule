/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.customization.impl.export;

import static org.mule.runtime.tracer.api.span.info.InitialExportInfo.DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;
import static org.mule.runtime.tracer.api.span.info.InitialExportInfo.NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.ASYNC_INNER_CHAIN_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.CACHE_CHAIN_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.MESSAGE_PROCESSORS_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.POLICY_CHAIN_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.POLICY_NEXT_ACTION_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.TRY_SCOPE_INNER_CHAIN_SPAN_NAME;
import static org.mule.runtime.tracer.customization.impl.info.SpanInitialInfoUtils.UNKNOWN;
import static org.mule.runtime.tracer.customization.impl.info.SpanInitialInfoUtils.getSpanName;

import static org.apache.commons.lang3.StringUtils.stripToEmpty;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.policy.PolicyChain;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.customization.api.InternalSpanNames;

import java.util.HashMap;
import java.util.Map;

/**
 * The monitoring level {@link InitialExportInfoProvider}.
 *
 * @since 4.6.0
 */
public class MonitoringInitialExportInfoProvider implements InitialExportInfoProvider {

  private final Map<Class, InitialExportInfo> initialExportInfoMapByComponentClass = new HashMap<Class, InitialExportInfo>() {

    {
      put(PolicyChain.class,
          new NoExportTillSpanWithNameInitialExportInfo(InternalSpanNames.EXECUTE_NEXT_SPAN_NAME, true));
    }
  };
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
    }
  };

  @Override
  public InitialExportInfo getInitialExportInfo(Component component) {
    return getInitialExportInfo(component, "");
  }

  @Override
  public InitialExportInfo getInitialExportInfo(Component component, String spanNameSuffix) {
    // This is done to resolve appropriately for inner classes related to policies
    // and will not be configurable.
    InitialExportInfo initialExportInfo = initialExportInfoMapByComponentClass.get(component.getClass());

    if (initialExportInfo != null) {
      return initialExportInfo;
    }

    return doGetInitialExportInfo(getSpanName(component.getIdentifier()) + stripToEmpty(spanNameSuffix));
  }

  @Override
  public InitialExportInfo getInitialExportInfo(String spanName) {
    return doGetInitialExportInfo(spanName);
  }

  private InitialExportInfo doGetInitialExportInfo(String componentFullyQualifiedName) {
    InitialExportInfo initialExportInfo = initialExportInfoMapByName.get(componentFullyQualifiedName);

    if (initialExportInfo == null) {
      initialExportInfo = DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;
    }

    return initialExportInfo;
  }
}
