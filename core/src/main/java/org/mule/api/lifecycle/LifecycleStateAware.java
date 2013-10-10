/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.lifecycle;

/**
 * Inject an objects lifecycle state.  This is useful for services that need to track or assert lifecycle state such as init, dispose
 * start, stop, dispose.
 *
 * @since 3.0
 */
public interface LifecycleStateAware
{
    void setLifecycleState(LifecycleState state);
}
