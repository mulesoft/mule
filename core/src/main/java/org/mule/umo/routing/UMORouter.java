/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.routing;

import org.mule.management.stats.RouterStatistics;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;

/**
 * <code>UMORouter</code> is a base interface for all routers.
 */
//public interface UMORouter extends Registerable
public interface UMORouter extends Initialisable, Disposable
{
    void setRouterStatistics(RouterStatistics stats);

    RouterStatistics getRouterStatistics();
}
