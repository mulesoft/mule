/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport;

import java.util.List;

/**
 * Tracks works that are running in behalf of a given component.
 */
public interface WorkTracker
{

    /**
     * Returns a list of works that are not completed.
     *
     * @return an immutable list of works
     */
    List<Runnable> pendingWorks();

    /**
     * Adds a work for tracking.
     *
     * @param work non null work.
     */
    void addWork(Runnable work);

    /**
     * Removes a work from tracking.
     *
     * @param work non null work
     */
    void removeWork(Runnable work);

}
