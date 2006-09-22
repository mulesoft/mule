/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.interceptors;

import org.mule.umo.UMOInterceptor;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;

/**
 * <code>LifecycleInterceptor</code> is a UMOInterceptor interface with two
 * additional lifecycle methods provided by <code>Initialisable</code> and
 * <code> Disposable
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface LifecycleInterceptor extends UMOInterceptor, Initialisable, Disposable
{
    // no methods
}
