/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.lifecycle;

import org.mule.api.MuleException;

/**
 * <code>Startable</code> provides an object with a {@link #start()} method
 * which gets called when the Mule instance gets started.  This is mostly used by
 * infrastructure components, but can also be implemented by service objects.
 *
 * This lifecycle interface should always be implemented with its opposite lifecycle interface {@link org.mule.api.lifecycle.Stoppable}.
 *
 * @see org.mule.api.lifecycle.Stoppable
 *
 */
public interface Startable
{
    String PHASE_NAME = "start";

    void start() throws MuleException;

    /**
     * Determines if this object is started or not
     */
    // TODO MULE-3969
    //boolean isStarted();
}
