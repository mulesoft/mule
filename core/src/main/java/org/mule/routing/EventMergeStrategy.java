/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.mule.VoidMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;

import java.util.List;

/**
 * Strategy pattern for merging a list of {@link MuleEvent} into a single one
 * 
 * @since 3.5.0
 */
public interface EventMergeStrategy
{

    /**
     * Merges the given list of events into a single one.
     * 
     * @param originalEvent the original event from which the given events were
     *            spawned. If not applicable to your case, provide a
     *            {@link VoidMuleEvent} instance
     * @param events the list of {@link MuleEvent} to be merged
     * @return a resulting {@link MuleEvent}. It can be a new event, an enriched
     *         version of the original event or whatever makes sense in your case. It
     *         cannot be <code>null</code>, return {@link VoidMuleEvent} instead
     * @throws MuleException
     */
    public MuleEvent merge(MuleEvent originalEvent, List<MuleEvent> events) throws MuleException;

}
