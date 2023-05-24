/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.customization.impl.info;

import static org.mule.runtime.tracer.customization.api.InternalSpanNames.OPERATION_EXECUTION_SPAN_NAME;
import static org.mule.runtime.tracer.customization.impl.info.SpanInitialInfoUtils.getLocationAsString;
import static org.mule.runtime.tracer.customization.impl.info.SpanInitialInfoUtils.getSpanName;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.policy.PolicyChain;
import org.mule.runtime.tracer.api.span.info.InitialExportInfo;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.customization.api.InitialExportInfoProvider;

import java.util.function.BiConsumer;

/**
 * An implementation of {@link InitialSpanInfo} for the execution.
 *
 * @since 4.5.0
 */
public class ExecutionInitialSpanInfo implements InitialSpanInfo {

  public static final String API_ID_ATTRIBUTE_KEY = "api.id";

  public static final String LOCATION_KEY = "location";

  public static final int INITIAL_ATTRIBUTES_BASE_COUNT = 1;

  public static final String EXECUTE_NEXT = "execute-next";
  public static final String FLOW = "flow";
  public static final String NO_LOCATION = "no-location";

  private final InitialExportInfo initialExportInfo;

  private final String name;
  // TODO: W-13313594 Should remove this property
  private final boolean isPolicySpan;
  private final boolean rootSpan;
  private final String location;
  private final String apiId;
  private int initialAttributesCount = INITIAL_ATTRIBUTES_BASE_COUNT;

  public ExecutionInitialSpanInfo(Component component, String apiId, InitialExportInfoProvider initialExportInfoProvider) {
    this(component, apiId, initialExportInfoProvider, null, "");
  }

  public ExecutionInitialSpanInfo(Component component, String apiId, String overriddenName,
                                  InitialExportInfoProvider initialExportInfoProvider) {
    this(component, apiId, initialExportInfoProvider, overriddenName, "");
  }

  public ExecutionInitialSpanInfo(Component component, String apiId, InitialExportInfoProvider initialExportInfoProvider,
                                  String overriddenName, String spanNameSuffix) {

    if (overriddenName == null) {
      name = getSpanName(component.getIdentifier()) + spanNameSuffix;
      initialExportInfo = initialExportInfoProvider.getInitialExportInfo(component);
    } else {
      name = overriddenName;
      initialExportInfo = initialExportInfoProvider.getInitialExportInfo(name);
    }
    this.isPolicySpan = isComponentOfName(component, EXECUTE_NEXT) || component instanceof PolicyChain
        || name.equals(OPERATION_EXECUTION_SPAN_NAME);
    this.rootSpan = isComponentOfName(component, FLOW);
    this.location = getLocationAsString(component.getLocation());
    this.apiId = apiId;
    if (apiId != null) {
      this.initialAttributesCount = INITIAL_ATTRIBUTES_BASE_COUNT + 1;
    }
  }

  @Override
  public void forEachAttribute(BiConsumer<String, String> biConsumer) {
    biConsumer.accept(LOCATION_KEY, location);
    if (apiId != null) {
      biConsumer.accept(API_ID_ATTRIBUTE_KEY, apiId);
    }
  }

  @Override
  public int getInitialAttributesCount() {
    return initialAttributesCount;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public InitialExportInfo getInitialExportInfo() {
    return initialExportInfo;
  }

  @Override
  public boolean isPolicySpan() {
    return isPolicySpan;
  }

  @Override
  public boolean isRootSpan() {
    return rootSpan;
  }

  private boolean isComponentOfName(Component component, String name) {
    return component.getIdentifier() != null && name.equals(component.getIdentifier().getName());
  }
}
