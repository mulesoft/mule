/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.routing;

import org.mule.runtime.core.api.event.BaseEvent;

import java.util.List;

/**
 * An SPI interface where custom logic can be plugged in to control how collections and single messages are returned from routers.
 */
public interface RouterResultsHandler {

  BaseEvent aggregateResults(List<BaseEvent> results, BaseEvent previous);
}
