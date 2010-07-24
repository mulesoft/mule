/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.expression;

import org.mule.api.MuleException;
import org.mule.api.RouterAnnotationParser;
import org.mule.api.annotations.routing.ExpressionFilter;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.routing.MessageFilter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;

/**
 * Responsible for converting a {@link org.mule.api.annotations.routing.ExpressionFilter} annotation to a
 * {@link org.mule.routing.filters.ExpressionFilter} instance for use on an inbound channel.
 */
public class ExpressionFilterAnnotationParser implements RouterAnnotationParser
{
    public MessageProcessor parseRouter(Annotation annotation) throws MuleException
    {
        MessageFilter router = new MessageFilter();
        Filter f = new ExpressionFilterParser().parseFilterString(((ExpressionFilter)annotation).value());
        router.setFilter(f);
        return null;//   router;
    }

    public boolean supports(Annotation annotation, Class clazz, Member member)
    {
        return annotation instanceof ExpressionFilter;
    }
}
