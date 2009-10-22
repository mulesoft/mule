/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.expression.parsers;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.api.RouterAnnotationParser;
import org.mule.api.routing.Router;
import org.mule.api.routing.filter.Filter;
import org.mule.config.annotations.routing.ExpressionFilter;
import org.mule.routing.filters.logic.AndFilter;
import org.mule.routing.filters.logic.OrFilter;
import org.mule.routing.inbound.SelectiveConsumer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.StringTokenizer;

/**
 * Responsible for converting a {@link org.mule.config.annotations.routing.ExpressionFilter} annotation to a
 * {@link org.mule.routing.filters.ExpressionFilter} instance for use on an inbound channel.
 */
public class ExpressionFilterAnnotationParser implements RouterAnnotationParser
{
    public Router parseRouter(Annotation annotation) throws MuleException
    {
        SelectiveConsumer router = new SelectiveConsumer();
        StringTokenizer st = new StringTokenizer(((ExpressionFilter)annotation).value(), "AND,OR", true);
        Filter f = new ExpressionFilterParser().parseFilterString(((ExpressionFilter)annotation).value());
        while ( st.hasMoreTokens())
        {
            String s = st.nextToken();
            if(s.equals("AND"))
            {
                f = new AndFilter(f);
            }
            else if(s.equals("OR"))
            {
                f = new OrFilter(f);
            }
            else if(f instanceof AndFilter)
            {
                ((AndFilter)f).getFilters().add(new org.mule.routing.filters.ExpressionFilter(s));
            }
            else if(f instanceof OrFilter)
            {
                ((OrFilter)f).getFilters().add(new org.mule.routing.filters.ExpressionFilter(s));
            }
            else if(f==null)
            {
                f = new org.mule.routing.filters.ExpressionFilter(s);
            }
            else
            {
                throw new DefaultMuleException("Expression Filter is malformed. IF this is a nested filter make sure each expression is separated by either 'AND' or 'OR'");
            }

        }

        router.setFilter(f);
        return router;
    }

    public boolean supports(Annotation annotation, Class clazz, Member member)
    {
        return annotation instanceof ExpressionFilter;
    }
}
