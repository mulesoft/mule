/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.lifecycle;

import org.mule.api.MuleException;

/**
 * This callback is used to execute lifecycle behaviour for an object being managed by a {@link LifecycleManager}
 * The callback is used so that transitions can be managed consistently outside of an object
 *
 * @since 3.0
 */
public interface LifecycleCallback<O>
{
    void onTransition(String phaseName, O object) throws MuleException;
}
