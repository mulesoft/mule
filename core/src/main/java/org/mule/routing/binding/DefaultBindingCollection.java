/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.binding;

import org.mule.api.routing.BindingCollection;
import org.mule.management.stats.RouterStatistics;
import org.mule.routing.AbstractRouterCollection;

/**
 * TODO
 */
public class DefaultBindingCollection extends AbstractRouterCollection implements BindingCollection
{

    public DefaultBindingCollection()
    {
        super(RouterStatistics.TYPE_INBOUND);
    }

}
