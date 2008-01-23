/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.nested;

import org.mule.api.routing.NestedRouterCollection;
import org.mule.management.stats.RouterStatistics;
import org.mule.routing.AbstractRouterCollection;

/**
 * TODO
 */
public class DefaultNestedRouterCollection extends AbstractRouterCollection implements NestedRouterCollection
{

    public DefaultNestedRouterCollection()
    {
        super(RouterStatistics.TYPE_INBOUND);
    }

}
