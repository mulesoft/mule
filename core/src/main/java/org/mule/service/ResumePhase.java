/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service;

import org.mule.lifecycle.phases.DefaultLifecyclePhase;

/**
 * Resume phase happens only alfter a pause phase.  Resume is a reverse lifecycle phase since it reverts the lifecycle state
 * to the state previous to the lifecycle pair invocation. i.e. if a services was started, then paused, then resumed, its lifecycle state
 * is reverted to 'start' not 'resume'.
 */
public class ResumePhase extends DefaultLifecyclePhase
{
    public ResumePhase()
    {
        super(Resumable.PHASE_NAME, Resumable.class, Pausable.PHASE_NAME);
    }
}
