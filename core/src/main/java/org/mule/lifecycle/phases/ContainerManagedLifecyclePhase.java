/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.lifecycle.phases;

/**
 * A container-managed lifecycle phase is called by the container itself. This class acts as a marker to enable
 * tracking of phases even if Mule does not initiate the phase
 */
public class ContainerManagedLifecyclePhase extends DefaultLifecyclePhase
{
    public ContainerManagedLifecyclePhase(String name, Class<?> lifecycleClass, String oppositePhaseName)
    {
        super(name, lifecycleClass, oppositePhaseName);
    }
}
