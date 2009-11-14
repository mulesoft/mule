/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json;

import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.NumberUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An expression evaluator to allow users to define json expressions in their mule configuration, i.e.
 * <code>
 * #[json:person->addresses[0]->postcode]
 * </code>
 *
 * See the {@link org.mule.module.json.JsonData} object for mor information about the query syntax.
 *
 * It is also possible to use this evaluator in {@link org.mule.routing.filters.ExpressionFilter} objects. For example
 * a filter could be defined as -
 *
 * <code>
 * #[json:person->registered]
 * </code>
 *
 * Where 'registered' is a boolean value.  It is also possible to filter on the existance of a value i.e.
 *
 * <code>
 * #[json:person->favouriteColour]
 * </code>
 *
 * Which would return true if 'favouriteColour' has been set. This evaluator also dds two logic operators you can use
 * to create more sophisticated boolean expressions; equals and not equals -
 *
 * <code>
 * #[json:person->favouriteColour == red]
 * </code>
 *
 * or
 *
 *  <code>
 * #[json:person->favouriteColour != brown]
 * </code>
 *
 * @see org.mule.module.json.JsonData
 */
public class JsonExpressionEvaluator implements ExpressionEvaluator
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(JsonExpressionEvaluator.class);

    public Object evaluate(String expression, MuleMessage message)
    {
        String compareTo = null;
        boolean not = false;
        int start = expression.lastIndexOf("/");
        if(start==-1) start = 0;
        int i = expression.indexOf("==", start);

        if(i > -1)
        {
            compareTo = expression.substring(i + 2, expression.length()).trim();
            expression = expression.substring(0, i).trim();
        }
        else if((i = expression.indexOf("!=", start)) > -1)
        {
            compareTo = expression.substring(i + 2, expression.length()).trim();
            expression = expression.substring(0, i).trim();
            not=true;
        }
        try
        {
            JsonData data = message.getPayload(JsonData.class);
            try
            {
                Object result = data.get(expression);
                if(compareTo!=null)
                {
                    if(compareTo.equalsIgnoreCase("null"))
                    {
                        boolean answer = result==null;
                        return (not ? !answer : answer);
                    }
                    else if(result instanceof Number && NumberUtils.isDigits(compareTo))
                    {
                        boolean answer = NumberUtils.createNumber(compareTo).equals(result);
                        return (not ? !answer : answer);
                    }
                    else if (result instanceof Boolean && (compareTo.equalsIgnoreCase("true") || compareTo.equalsIgnoreCase("false")))
                    {
                        boolean answer = result.equals(Boolean.valueOf(compareTo));
                        return (not ? !answer : answer);
                    }
                    else
                    {
                        boolean answer = compareTo.equals(result);
                        return (not ? !answer : answer);                        
                    }
                }
                else
                {
                    return result;
                }
            }
            catch (IllegalArgumentException e)
            {
                logger.debug("returning null for json expression: " + expression + ": " + e.getMessage());
                return null;
            }
        }
        catch (TransformerException e)
        {
            throw new MuleRuntimeException(CoreMessages.failedToProcessExtractorFunction(getName()+ ":" + expression), e);
        }
    }

    public void setName(String name)
    {
        throw new UnsupportedOperationException("setName");
    }

    public String getName()
    {
        return "json";
    }
}
