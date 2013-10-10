/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
