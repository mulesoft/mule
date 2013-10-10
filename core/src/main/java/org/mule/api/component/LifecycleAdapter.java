/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.component;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.service.Service;

/**
 * <code>LifecycleAdapter</code> is a wrapper around a pojo service that adds
 * Lifecycle methods to the pojo. It also associates the pojo service with its
 * {@link Service} object.
 * 
 * @see Service
 */
public interface LifecycleAdapter extends Lifecycle
{
    boolean isStarted();

    boolean isDisposed();

    Object invoke(MuleEvent message) throws MuleException;

}
