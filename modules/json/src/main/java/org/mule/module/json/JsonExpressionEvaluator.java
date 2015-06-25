/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json;

import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.expression.AbstractExpressionEvaluator;
import org.mule.util.NumberUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.ValueNode;

/**
 * An expression evaluator to allow users to define json expressions in their mule configuration, i.e.
 * <code>
 * #[json:person/addresses[0]/postcode]
 * </code>
 * <p/>
 * See the {@link org.mule.module.json.JsonData} object for mor information about the query syntax.
 * <p/>
 * It is also possible to use this evaluator in {@link org.mule.routing.filters.ExpressionFilter} objects. For example
 * a filter could be defined as -
 * <p/>
 * <code>
 * #[json:person/registered]
 * </code>
 * <p/>
 * Where 'registered' is a boolean value.  It is also possible to filter on the existence of a value i.e.
 * <p/>
 * <code>
 * #[json:person/favouriteColour]
 * </code>
 * <p/>
 * Which would return true if 'favouriteColour' has been set. This evaluator also dds two logic operators you can use
 * to create more sophisticated boolean expressions; equals and not equals -
 * <p/>
 * <code>
 * #[json:person/favouriteColour = red]
 * </code>
 * <p/>
 * or
 * <p/>
 * <code>
 * #[json:person/favouriteColour != brown]
 * </code>
 *
 * @see org.mule.module.json.JsonData
 */
public class JsonExpressionEvaluator extends AbstractExpressionEvaluator
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(JsonExpressionEvaluator.class);

    @Override
    public Object evaluate(String expression, MuleMessage message)
    {
        String compareTo = null;
        boolean not = false;
        int start = expression.lastIndexOf("/");
        if (start == -1)
        {
            start = 0;
        }
        int i=0;
        if ((i = expression.indexOf("!=", start)) > -1)
        {
            compareTo = expression.substring(i + 2, expression.length()).trim();
            expression = expression.substring(0, i).trim();
            not = true;
        }
        else if ((i = expression.indexOf("=", start)) > -1)
        {
            compareTo = expression.substring(i + 1, expression.length()).trim();
            expression = expression.substring(0, i).trim();
        }
        
        try
        {
            String json = message.getPayloadAsString();
            JsonData data = new JsonData(json);
            try
            {
                JsonNode resultNode = data.get(expression);
                if (compareTo != null)
                {
                    Object resultValue = resultNode.isValueNode() ? resultNode.asText() : resultNode;
                    if (compareTo.equalsIgnoreCase("null"))
                    {
                        boolean answer = resultValue == null;
                        return (not ? !answer : answer);
                    }
                    else if (resultValue instanceof Number && NumberUtils.isDigits(compareTo))
                    {
                        boolean answer = NumberUtils.createNumber(compareTo).equals(resultValue);
                        return (not ? !answer : answer);
                    }
                    else if (resultValue instanceof Boolean
                             && (compareTo.equalsIgnoreCase("true") || compareTo.equalsIgnoreCase("false")))
                    {
                        boolean answer = resultValue.equals(Boolean.valueOf(compareTo));
                        return (not ? !answer : answer);
                    }
                    else
                    {
                        boolean answer = compareTo.equals(resultValue);
                        return (not ? !answer : answer);
                    }
                }
                else
                {
                    return extractResultFromNode(resultNode);
                }
            }
            catch (IllegalArgumentException e)
            {
                if (compareTo == null)
                {
                    logger.debug("returning null for json expression: " + expression + ": " + e.getMessage());
                    return null;
                }
                //If the element does not exist but is matching against 'null' return true, otherwise false
                return (compareTo.equalsIgnoreCase("null")) & !not;
            }
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(CoreMessages.failedToProcessExtractorFunction(getName() + ":" + expression), e);
        }
    }

    protected Object extractResultFromNode(JsonNode result)
    {
        if (result instanceof ValueNode)
        {
            return result.asText();
        }
        if (result instanceof ObjectNode)
        {
            return ((ObjectNode) result).toString();
        }
        else if (result instanceof ArrayNode)
        {
            List<Object> parts = new ArrayList<Object>();
            for (Iterator<JsonNode> i = result.getElements(); i.hasNext();)
            {
                JsonNode arrayNode = i.next();
                parts.add(extractResultFromNode(arrayNode));
            }
            return parts;
        }
        else
        {
            return result;
        }
    }
    
    @Override
    public String getName()
    {
        return "json";
    }
}
