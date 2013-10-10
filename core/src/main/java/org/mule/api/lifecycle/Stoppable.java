/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    
    void stop() throws MuleException;
}
