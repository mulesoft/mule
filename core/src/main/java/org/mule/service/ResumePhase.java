/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service;

import org.mule.api.lifecycle.ReverseLifecyclePhase;
import org.mule.lifecycle.DefaultLifecyclePhase;

/**
 * Resume phase happens only alfter a pause phase.  Resume is a reverse lifecycle phase since it reverts the lifecycle state
 * to the state previous to the lifecycle pair invocation. i.e. if a services was started, then paused, then resumed, its lifecycle state
 * is reverted to 'start' not 'resume'.
 */
public class ResumePhase extends DefaultLifecyclePhase implements ReverseLifecyclePhase
{
    public ResumePhase()
    {
        super(Resumable.PHASE_NAME, Resumable.class, Pausable.PHASE_NAME);
    }
}