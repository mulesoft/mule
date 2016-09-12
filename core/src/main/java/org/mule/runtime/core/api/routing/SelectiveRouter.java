/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.routing;

import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.filter.Filter;

/**
 * Routes the event to <code>MessageProcessor</code>s using a {@link Filter} to evaluate the event being processed and determine
 * if a given route should be used.
 * <p>
 * If the implementation supports the use of a default route then this will be used to route any events that don't match any other
 * routes.
 */
public interface SelectiveRouter extends Processor {

  void addRoute(Processor processor, Filter filter);

  void updateRoute(Processor processor, Filter filter);

  void removeRoute(Processor processor);

  void setDefaultRoute(Processor processor);
}
