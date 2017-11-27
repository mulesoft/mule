/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.pool;

/**
 * <code>CancellableRunnable</code> defines an interface where
 * some logic can be executed in case the work may be cancelled
 * by a rejection policy.
 */
public abstract class CancellableRunnable implements Runnable
{
    public abstract void cancel();
}
