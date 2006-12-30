/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.lifecycle;

import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOInterceptor;

/**
 * <code>UMOLifecycleAdapter</code> TODO (document class)
 */
public interface UMOLifecycleAdapter extends Lifecycle, Initialisable, UMOInterceptor
{
    boolean isStarted();

    boolean isDisposed();

    UMODescriptor getDescriptor();
}
