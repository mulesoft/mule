/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.pool;

import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;

/**
 * An ObjectPool that allows Start and Stop life-cycle to be propagated pooled
 * object.
 */
public interface LifecyleEnabledObjectPool extends ObjectPool, Startable, Stoppable
{

}
