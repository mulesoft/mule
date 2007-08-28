/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.lifecycle.phases;

import org.mule.impl.lifecycle.LifecyclePhase;
import org.mule.umo.lifecycle.UMOLifecyclePhase;

/**
 * This lifecycle phase marks the 'pre-lifecycle' phase of an object. The default phase before
 * any other phase has been executed
 */
public class NotInLifecyclePhase extends LifecyclePhase
{
    public static String PHASE_NAME = "not in lifecycle";

    public NotInLifecyclePhase()
    {
        super(PHASE_NAME, NotInLifecyclePhase.class, null);
        registerSupportedPhase(UMOLifecyclePhase.ALL_PHASES);
    }
}