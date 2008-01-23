/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.routing;

import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.management.stats.RouterStatistics;

/**
 * <code>Router</code> is a base interface for all routers.
 */
//public interface Router extends Registerable
public interface Router extends Initialisable, Disposable
{
    void setRouterStatistics(RouterStatistics stats);

    RouterStatistics getRouterStatistics();
}
