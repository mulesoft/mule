/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.lifecycle;

import org.mule.umo.UMOComponent;
import org.mule.umo.UMOInterceptor;

/**
 * <code>UMOLifecycleAdapter</code> is a wrapper around a pojo service that adds Lifecycle methods to the pojo. It also
 * associates the pojo service with its {@link UMOComponent} object.
 *
 * @see UMOComponent
 */
public interface UMOLifecycleAdapter extends Lifecycle, UMOInterceptor
{
    boolean isStarted();

    boolean isDisposed();

    UMOComponent getComponent();
}
