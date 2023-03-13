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
 * A provider for builders for {@link InitialSpanInfo}
 *
 * @since 4.6.0
 */
public interface InitialSpanInfoBuilderProvider {

  /**
   * @return a builder for {@link InitialSpanInfo} based on a {@link Component}.
   */
  InitialSpanInfoBuilder getComponentInitialSpanInfoBuilder(Component component);

  /**
   * @return a builder for generic {@link InitialSpanInfo}.
   */
  InitialSpanInfoBuilder getGenericInitialSpanInfoBuilder();
}
