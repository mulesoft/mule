/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.configuration.internal.builder;

import static org.apache.commons.lang3.StringUtils.stripToEmpty;

import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.configuration.api.InitialSpanInfoBuilder;
import org.mule.runtime.tracer.configuration.internal.info.FixedNameInitialSpanInfo;
import org.mule.runtime.tracer.configuration.internal.info.NoExportFixedNameInitialSpanInfo;
import org.mule.runtime.tracer.configuration.internal.info.FixedNamedNoExportTillComponentFoundExecutionInitialSpanInfo;

/**
 * A generic {@link InitialSpanInfoBuilder} that is not based on any component.
 *
 * @since 4.6.0
 */
public class GenericInitialSpanInfoBuilder implements InitialSpanInfoBuilder {

  private String name;
  private String suffix;
  private String noExportUntilComponentName;
  private boolean noExport = false;

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
  public InitialSpanInfoBuilder withForceNoExportUntil(String noExportUntilComponentName) {
    this.noExportUntilComponentName = noExportUntilComponentName;
    return this;
  }

  @Override
  public InitialSpanInfo build() {
    if (name == null) {
      throw new IllegalArgumentException("The name cannot be null");
    }

    if (noExportUntilComponentName != null) {
      return new FixedNamedNoExportTillComponentFoundExecutionInitialSpanInfo(getSpanName(), noExportUntilComponentName);
    }

    if (noExport) {
      return new NoExportFixedNameInitialSpanInfo(getSpanName());
    }

    return new FixedNameInitialSpanInfo(getSpanName());
  }

  private String getSpanName() {
    return name + stripToEmpty(suffix);
  }

  @Override
  public InitialSpanInfoBuilder withNoExport() {
    this.noExport = true;
    return this;
  }
}
