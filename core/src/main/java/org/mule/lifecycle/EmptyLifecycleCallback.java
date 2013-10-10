/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.lifecycle;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.LifecycleCallback;

/**
 * A lifecycle callback that does nothing. Can be used to transition a {@link org.mule.api.lifecycle.LifecycleManager} to
 * the next phase without executing logic.
 *
 * USers should never use this object themselves, it provides an internal Mule function.
 *
 * @since 3.0
 */
public class EmptyLifecycleCallback<O> implements LifecycleCallback<O>
{
    public void onTransition(String phaseName, O object) throws MuleException
    {
        //do nothing
    }
}
