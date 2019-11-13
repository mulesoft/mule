/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.util.List;

/**
 * Defines an strategy to route a {@link CoreEvent} through a set of
 * {@link org.mule.runtime.core.api.processor.Processor}
 */
public interface RoutingStrategy {

  /**
   * Routes {@link CoreEvent} through a set of {@link Processor}
   *
   * @param event
   * @param messageProcessors
   * @return
   */
  CoreEvent route(CoreEvent event, List<Processor> messageProcessors) throws MuleException;
}
