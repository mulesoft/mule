/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.converters;

import org.mule.api.MuleContext;
import org.mule.api.expression.PropertyConverter;
import org.mule.api.routing.filter.Filter;
import org.mule.expression.ExpressionConfig;
import org.mule.routing.filters.ExpressionFilter;

/**
 * Converts an Expression sting into a Filter object. The string must define an expression that results in a
 * boolean value.
 */
public class FilterConverter implements PropertyConverter
{
    public Object convert(String property, MuleContext context)
    {
        if (null != property)
        {
            ExpressionConfig config = new ExpressionConfig();
            config.parse(property);
            ExpressionFilter filter = new ExpressionFilter(config.getExpression(), config.getEvaluator(), config.getCustomEvaluator());
            filter.setMuleContext(context);
            return filter;
        }
        else
        {
            return null;
        }

    }

    public Class getType()
    {
        return Filter.class;
    }
}
