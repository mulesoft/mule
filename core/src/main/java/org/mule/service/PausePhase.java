/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
