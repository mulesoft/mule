/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
