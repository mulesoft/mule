/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.configuration.api;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;

/**
 * A builder for {@link InitialSpanInfo} based on a {@link Component}.
 *
 * @since 4.6.0
 */
public interface InitialSpanInfoBuilder {

  /**
   * Sets the name.
   *
   * @param name the name for building the component initial span.
   *
   * @return {@link InitialSpanInfoBuilder}.
   */
  InitialSpanInfoBuilder withName(String name);

  /**
   * Sets the suffix.
   *
   * @param suffix the suffix to use.
   *
   * @return the {@link InitialSpanInfoBuilder}
   */
  InitialSpanInfoBuilder withSuffix(String suffix);

  /**
   * Indicates that no children will be exported till a span is found with the component name passed as parameter.
   *
   * @param componentName the component name when this will start exporting again
   *
   * @return he {@link InitialSpanInfoBuilder}
   */
  InitialSpanInfoBuilder withForceNotExportUntil(String componentName);

  /**
   * @return the resulting {@link InitialSpanInfo}.
   */
  InitialSpanInfo build();

  /**
   * Indicates that the export should not be exported.
   *
   * @return the resulting {@link InitialSpanInfo}.
   */
  InitialSpanInfoBuilder withNoExport();
}
