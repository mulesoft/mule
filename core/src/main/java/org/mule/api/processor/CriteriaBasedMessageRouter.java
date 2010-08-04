/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.processor;

import org.mule.api.Criteria;

/**
 * A {@link MessageProcessor} that routes messages to zero or more destination message processors using the
 * specified criteria to determine if a router should be used not not. Implementatios determine the type of
 * {@link Criteria} and how it is used as well as if the first route is used or if all valid routes are used.
 */
public interface CriteriaBasedMessageRouter<T extends Criteria> extends MessageProcessor
{
    /**
     * Adds a new message processor to the list of routes
     * 
     * @param processor new destination message processor
     */
    void addRoute(MessageProcessor processor, T criteria);

    /**
     * Removes a message processor from the list of routes
     * 
     * @param processor destination message processor to remove
     * @return true if the route was removed
     */
    boolean removeRoute(MessageProcessor processor);

}
