/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.dsl.routers;

import org.mule.config.dsl.OutRouteBuilder;
import org.mule.routing.filters.ExpressionFilter;

/**
 * TODO
 */
@Deprecated
public class WhenDefinition
{
    private ContentBasedRouter router;

    WhenDefinition(ContentBasedRouter router)
    {
        this.router = router;
    }

    public OutRouteBuilder when(String expression)
    {
        ExpressionFilter filter = new ExpressionFilter(expression);
        //filter.setMuleContext(mule);
        WhenDefinition wd = new WhenDefinition(router);

        return null;
    }
}
