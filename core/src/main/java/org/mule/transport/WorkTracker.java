/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import org.mule.api.lifecycle.Disposable;

import java.util.List;

/**
 * Tracks works that are running in behalf of a given component.
 */
public interface WorkTracker extends Disposable
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
