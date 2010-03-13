/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.lifecycle;

/**
 * A coupling of two opposite {@link org.mule.api.lifecycle.LifecyclePhase} instances such as initialise and dispose or start and stop
 * Mule treats lifecycle in pairs in order to return system state to a level consistency when an opposite lifecycle method is called.
 *
 * @since 3.0
 */
public interface LifecyclePair
{
    /**
     * The beginning lifecycle phase in the pair i.e. initialise or start
     * @return The beginning lifecycle phase in the pair
     */
    LifecyclePhase getBegin();

    /**
     * The end lifecycle phase in the pair i.e. dispose or stop
     * @return The end lifecycle phase in the pair
     */
    LifecyclePhase getEnd();
}
