/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.configuration.internal.export;

import static org.mule.runtime.tracer.api.span.info.InitialExportInfo.DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;
import static org.mule.runtime.tracer.api.span.info.InitialExportInfo.NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;
import static org.mule.runtime.tracer.configuration.api.InternalSpanNames.ASYNC_INNER_CHAIN;
import static org.mule.runtime.tracer.configuration.api.InternalSpanNames.MULE_CACHE_CHAIN;
import static org.mule.runtime.tracer.configuration.api.InternalSpanNames.MULE_MESSAGE_PROCESSORS;
import static org.mule.runtime.tracer.configuration.api.InternalSpanNames.MULE_POLICY_CHAIN_INITIAL_EXPORT_INFO_KEY;
import static org.mule.runtime.tracer.configuration.api.InternalSpanNames.MULE_POLICY_NEXT_ACTION_EXPORT_INFO_KEY;
import static org.mule.runtime.tracer.configuration.internal.info.SpanInitialInfoUtils.UNKNOWN;
import static org.mule.runtime.tracer.configuration.internal.info.SpanInitialInfoUtils.getSpanName;

import static org.apache.commons.lang3.StringUtils.stripToEmpty;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.policy.PolicyChain;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.configuration.api.InternalSpanNames;
import org.mule.runtime.tracer.configuration.internal.info.NoExportTillSpanWithNameInitialExportInfo;

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
          new NoExportTillSpanWithNameInitialExportInfo(InternalSpanNames.EXECUTE_NEXT_COMPONENT_NAME, true));
    }
  };
  private final Map<String, InitialExportInfo> initialExportInfoMapByName = new HashMap<String, InitialExportInfo>() {

    {
      put(MULE_POLICY_CHAIN_INITIAL_EXPORT_INFO_KEY,
          new NoExportTillSpanWithNameInitialExportInfo(InternalSpanNames.EXECUTE_NEXT_COMPONENT_NAME, true));
      put(MULE_POLICY_NEXT_ACTION_EXPORT_INFO_KEY, NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO);
      put(UNKNOWN, NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO);
      put(ASYNC_INNER_CHAIN, NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO);
      put(MULE_CACHE_CHAIN, NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO);
      put(MULE_MESSAGE_PROCESSORS, NO_EXPORTABLE_DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO);
    }
  };

  @Override
  public InitialExportInfo getInitialExportInfo(Component component) {
    return getInitialExportInfo(component, "");
  }

  @Override
  public InitialExportInfo getInitialExportInfo(Component component, String suffix) {
    // This is done to resolve appropriately for inner classes related to policies
    // and will not be configurable.
    InitialExportInfo initialExportInfo = initialExportInfoMapByComponentClass.get(component.getClass());

    if (initialExportInfo != null) {
      return initialExportInfo;
    }

    return doGetInitialExportInfo(getSpanName(component.getIdentifier()) + stripToEmpty(suffix));
  }

  @Override
  public InitialExportInfo getInitialExportInfo(String name) {
    return doGetInitialExportInfo(name);
  }

  private InitialExportInfo doGetInitialExportInfo(String componentFullyQualifiedName) {
    InitialExportInfo initialExportInfo = initialExportInfoMapByName.get(componentFullyQualifiedName);

    if (initialExportInfo == null) {
      initialExportInfo = DEFAULT_EXPORT_SPAN_CUSTOMIZATION_INFO;
    }

    return initialExportInfo;
  }
}
