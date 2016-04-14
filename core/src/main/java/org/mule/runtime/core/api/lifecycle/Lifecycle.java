/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.lifecycle;

/**
 * <code>DefaultLifecyclePhase</code> adds lifecycle methods <code>start</code>,
 * <code>stop</code> and <code>dispose</code>.
 */
public interface Lifecycle extends Initialisable, Startable, Stoppable, Disposable
{
    // empty
}
