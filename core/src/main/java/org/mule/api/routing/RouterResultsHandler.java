/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
