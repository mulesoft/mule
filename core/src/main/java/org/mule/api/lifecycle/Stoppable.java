/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.lifecycle;

import org.mule.api.MuleException;

/**
 * <code>Stoppable</code> is a lifecycle interfaqce that introduces a {@link #stop()} method to an object.
 *
 * This lifecycle interface should always be implemented with its opposite lifecycle interface {@link org.mule.api.lifecycle.Stoppable}.
 *
 * @see org.mule.api.lifecycle.Startable
 */
public interface Stoppable
{
    String PHASE_NAME = "stop";

    /**
     * Stops a lifecycle component.
     *
     * <p>
     * Note: in most cases the correct context has to be set before stopping the component. 
     * Special attention has to be paid to the classloader from which the component is 
     * stopped.
     * </p>
     * 
     * @throws MuleException exception during the start process.
     */
    void stop() throws MuleException;
}
