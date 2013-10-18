/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.spi;

import org.mule.api.MuleContext;
import org.mule.routing.filters.ExpressionFilter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.ibeans.annotation.filter.AtomErrorFilter;
import org.ibeans.annotation.filter.ExpressionErrorFilter;
import org.ibeans.annotation.filter.JsonErrorFilter;
import org.ibeans.annotation.filter.RssErrorFilter;
import org.ibeans.annotation.filter.XmlErrorFilter;
import org.ibeans.api.IBeansException;
import org.ibeans.api.channel.MimeType;
import org.ibeans.spi.ErrorFilter;
import org.ibeans.spi.ErrorFilterFactory;

/**
 * An expression filter factory that can build filters for the following annotations {@link org.ibeans.annotation.filter.ExpressionErrorFilter},
 * {@link org.ibeans.annotation.filter.XmlErrorFilter}, {@link org.ibeans.annotation.filter.AtomErrorFilter}, {@link org.ibeans.annotation.filter.RssErrorFilter}, and
 * {@link org.ibeans.annotation.filter.JsonErrorFilter}
 *
 * @see org.ibeans.spi.ErrorFilter
 */
public class ExpressionErrorFilterFactory implements ErrorFilterFactory
{
    private MuleContext muleContext;

    public ExpressionErrorFilterFactory(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public boolean isSupported(Annotation annotation)
    {
        return (annotation instanceof ExpressionErrorFilter ||
                annotation instanceof XmlErrorFilter ||
                annotation instanceof AtomErrorFilter ||
                annotation instanceof RssErrorFilter ||
                annotation instanceof JsonErrorFilter);
    }

    public ErrorFilterFactory.ErrorFilterHolder parse(Annotation anno) throws IBeansException
    {
        ErrorFilter errorFilter;
        try
        {
            String expr = (String) anno.annotationType().getMethod("expr").invoke(anno);
            String errorCode = (String) anno.annotationType().getMethod("errorCode").invoke(anno);
            String tempMime = (String) anno.annotationType().getMethod("mimeType").invoke(anno);
            MimeType mimeType = new MimeType(tempMime);

            Field f;
            String evaluator;

            try
            {
                f = anno.annotationType().getDeclaredField("eval");
                evaluator = (String) f.get(anno);
            }
            catch (NoSuchFieldException nsfe)
            {
                //This is a custom ExpressionErrorFilter
                evaluator = (String) anno.annotationType().getMethod("eval").invoke(anno);
            }

            //special handling for xpath queries
            if (evaluator.equals("xpath2") &&! expr.startsWith("[boolean]"))
            {
                expr = "[boolean]" + expr;
            }
            errorFilter = new ErrorExpressionFilter(evaluator, expr, errorCode);
            ((ExpressionFilter)errorFilter).setMuleContext(muleContext);
            return new ErrorFilterHolder(mimeType, errorFilter);
        }
        catch (Exception e)
        {
            throw new IBeansException("Failed to parse error filter from annotation: " + anno, e);
        }
    }
}
