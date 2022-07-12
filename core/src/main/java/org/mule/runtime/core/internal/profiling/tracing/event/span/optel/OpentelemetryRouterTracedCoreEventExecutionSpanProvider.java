/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span.optel;

import org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventExecutionSpanProvider;

/**
 * A {@link CoreEventExecutionSpanProvider} for routers.
 *
 * @since 4.5.0
 */
public class OpentelemetryRouterTracedCoreEventExecutionSpanProvider
    extends AbstractOpentelemetryTracedCoreEventExecutionSpanProvider {

  public static final String ROUTER_SPAN_PROVIDER_SPAN_NAME = "router";


  @Override
  public String getComponentSubTaskSuffix() {
    return CORE_EVENT_SPAN_NAME_SEPARATOR + ROUTER_SPAN_PROVIDER_SPAN_NAME;
  }
}
