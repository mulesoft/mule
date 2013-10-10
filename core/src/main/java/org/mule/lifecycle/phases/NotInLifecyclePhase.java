/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.lifecycle.phases;

import org.mule.api.lifecycle.Initialisable;

/**
 * This lifecycle phase marks the 'pre-lifecycle' phase of an object. The default phase before
 * any other phase has been executed
 */
public class NotInLifecyclePhase extends DefaultLifecyclePhase
{
    public static final String PHASE_NAME = "not in lifecycle";

    public NotInLifecyclePhase()
    {
        super(PHASE_NAME, NotInLifecyclePhase.class, null);
        registerSupportedPhase(Initialisable.PHASE_NAME);
    }
}
