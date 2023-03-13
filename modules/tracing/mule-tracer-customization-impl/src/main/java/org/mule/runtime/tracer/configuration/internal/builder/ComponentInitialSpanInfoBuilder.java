/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.configuration.internal.builder;

import static org.apache.commons.lang3.StringUtils.stripToEmpty;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.configuration.api.InitialSpanInfoBuilder;
import org.mule.runtime.tracer.configuration.internal.info.ComponentExecutionInitialSpanInfo;
import org.mule.runtime.tracer.configuration.internal.info.FixedComponentCoreExecutionInitialSpanInfo;
import org.mule.runtime.tracer.configuration.internal.info.NoExportComponentExecutionInitialSpanInfo;
import org.mule.runtime.tracer.configuration.internal.info.NoExportTillComponentFoundExecutionInitialSpanInfo;

/**
 * A {@link Component}-based {@link InitialSpanInfoBuilder}.
 *
 * @since 4.6.0
 */
public class ComponentInitialSpanInfoBuilder implements InitialSpanInfoBuilder {

  private final Component component;
  private String name;
  private String suffix;
  private String forceNotExportUntilComponentName;
  private boolean noExport = false;

  public ComponentInitialSpanInfoBuilder(Component component) {
    this.component = component;
  }

  @Override
  public InitialSpanInfoBuilder withName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public InitialSpanInfoBuilder withSuffix(String suffix) {
    this.suffix = suffix;
    return this;
  }

  @Override
  public InitialSpanInfoBuilder withForceNotExportUntil(String forceNotExportUntilComponentName) {
    this.forceNotExportUntilComponentName = forceNotExportUntilComponentName;
    return this;
  }

  @Override
  public InitialSpanInfo build() {
    if (forceNotExportUntilComponentName != null) {
      return new NoExportTillComponentFoundExecutionInitialSpanInfo(component, forceNotExportUntilComponentName);
    }

    if (noExport) {
      return new NoExportComponentExecutionInitialSpanInfo(component);
    }

    if (name != null) {
      return new FixedComponentCoreExecutionInitialSpanInfo(component, name + stripToEmpty(suffix));
    }

    return new ComponentExecutionInitialSpanInfo(component, stripToEmpty(suffix));

  }

  @Override
  public InitialSpanInfoBuilder withNoExport() {
    this.noExport = true;
    return this;
  }
}
