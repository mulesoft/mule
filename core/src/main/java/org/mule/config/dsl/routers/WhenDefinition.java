/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.dsl.routers;

import org.mule.config.dsl.OutRouteBuilder;
import org.mule.routing.filters.ExpressionFilter;

/**
 * TODO
 */
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
