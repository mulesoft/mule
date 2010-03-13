/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.lifecycle;

import org.mule.api.lifecycle.LifecyclePair;
import org.mule.api.lifecycle.LifecyclePhase;

/**
 * A coupling of two opposite {@link org.mule.api.lifecycle.LifecyclePhase} instances such as initialise and dispose or start and stop
 * Mule treats lifecycle in pairs in order to return system state to a level consistency when an opposite lifecycle method is called.
 */
public class DefaultLifecyclePair implements LifecyclePair
{
    private LifecyclePhase begin;
    private LifecyclePhase end;

    public DefaultLifecyclePair(LifecyclePhase begin, LifecyclePhase end)
    {
        this.begin = begin;
        this.end = end;
    }

    public LifecyclePhase getBegin()
    {
        return begin;
    }

    public LifecyclePhase getEnd()
    {
        return end;
    }
}
