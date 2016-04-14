/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
