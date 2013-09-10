/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.expression;

import org.mule.api.DefaultMuleException;
import org.mule.api.routing.filter.Filter;
import org.mule.routing.filters.logic.AndFilter;
import org.mule.routing.filters.logic.OrFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Will create a filter from one of more expression filters.  This parser will parse one or more filter expressions
 * and understands the operators AND and OR. i.e.
 *
 * #[regex:.* bar] OR #[wildcard:foo*]
 *
 * or
 *
 * #[xpath:/Order/id != null] AND #[header:foo=bar]
 */
public class ExpressionFilterParser
{
    public Filter parseFilterString(String filterString) throws DefaultMuleException
    {
        List<String> strings = split(filterString);
        Filter filter = null;

        for (String s : strings)
        {
            s = s.trim();
            if (s.equals("AND"))
            {
                filter = new AndFilter(filter);
            }
            else if (s.equals("OR"))
            {
                filter = new OrFilter(filter);
            }
            else if (filter instanceof AndFilter)
            {
                ((AndFilter) filter).getFilters().add(new org.mule.routing.filters.ExpressionFilter(s));
            }
            else if (filter instanceof OrFilter)
            {
                ((OrFilter) filter).getFilters().add(new org.mule.routing.filters.ExpressionFilter(s));
            }
            else if (filter == null)
            {
                filter = new org.mule.routing.filters.ExpressionFilter(s);
            }
            else
            {
                throw new DefaultMuleException("Expression Filter is malformed. IF this is a nested filter make sure each expression is separated by either 'AND' or 'OR'");
            }
        }
        return filter;
    }

    protected List<String> split(String string)
    {
        List<String> strings = new ArrayList<String>();
        int i = 0;

        while(i> -1)
        {
            int a = string.indexOf("AND", i);
            int o = string.indexOf("OR", i);
            if(a > 1)
            {
                strings.add(string.substring(0, a));
                strings.add("AND");
                string = string.substring(a + 4).trim();

            }
            else if(o > 1)
            {
                strings.add(string.substring(0, o));
                strings.add("OR");
                string = string.substring(o + 3).trim();
            }
            else
            {
                strings.add(string);
                i = -1;
            }
        }
        return strings;
    }
}
