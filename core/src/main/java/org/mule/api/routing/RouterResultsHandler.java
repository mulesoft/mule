/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.routing;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;

import java.util.List;

/**
 * An SPI interface where custom logic can be plugged in to control how collections and single messages
 * are returned from routers.
 */
public interface RouterResultsHandler
{
    MuleEvent aggregateResults(List<MuleEvent> results, MuleEvent previous, MuleContext muleContext);
}
