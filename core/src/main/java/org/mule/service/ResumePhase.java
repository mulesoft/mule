/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
