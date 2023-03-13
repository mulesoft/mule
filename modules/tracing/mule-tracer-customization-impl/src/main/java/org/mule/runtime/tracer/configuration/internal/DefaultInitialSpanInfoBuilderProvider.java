/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.configuration.internal;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.tracer.configuration.internal.builder.GenericInitialSpanInfoBuilder;
import org.mule.runtime.tracer.configuration.api.InitialSpanInfoBuilder;
import org.mule.runtime.tracer.configuration.api.InitialSpanInfoBuilderProvider;
import org.mule.runtime.tracer.configuration.internal.builder.ComponentInitialSpanInfoBuilder;

/**
 * Default implementation of {@link InitialSpanInfoBuilderProvider}
 *
 * @since 4.6.0
 */
public class DefaultInitialSpanInfoBuilderProvider implements InitialSpanInfoBuilderProvider {

  @Override
  public InitialSpanInfoBuilder getComponentInitialSpanInfoBuilder(Component component) {
    return new ComponentInitialSpanInfoBuilder(component);
  }

  @Override
  public InitialSpanInfoBuilder getGenericInitialSpanInfoBuilder() {
    return new GenericInitialSpanInfoBuilder();
  }
}
