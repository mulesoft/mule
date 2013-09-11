/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.stats;

import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;

/**
 * Watches {@link ProcessingTime} instances to detect when they are weakly
 * reachable.
 */
public interface ProcessingTimeWatcher extends Startable, Stoppable
{

    /**
     * Adds a new instance to watch
     *
     * @param processingTime instance to add. Non null
     */
    void addProcessingTime(ProcessingTime processingTime);

}
