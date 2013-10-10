/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.service;

import org.mule.lifecycle.phases.DefaultLifecyclePhase;

/**
 * Defines a phase phase that will invoke the {@link org.mule.service.Pausable#pause()} method on an object.
 * This phase must be paired with the {@link Resumable} phase.
 */
public class PausePhase extends DefaultLifecyclePhase
{
    public PausePhase()
    {
        super(Pausable.PHASE_NAME, Pausable.class, Resumable.PHASE_NAME);
    }
}
