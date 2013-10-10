/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.json;

import org.mule.routing.AbstractSplitter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ValueNode;

/**
 * An JSON expression evaluator that returns {@link JsonNode}'s instead of strings.
 * Arrays are still returned as a {@link List} rather than as an {@link ArrayNode} to
 * enable splitting using the {@link AbstractSplitter}
 * 
 * @see org.mule.module.json.JsonData
 * @see JsonExpressionEvaluator
 */
public class JsonNodeExpressionEvaluator extends JsonExpressionEvaluator
{
    @Override
    protected Object extractResultFromNode(JsonNode result)
    {
        if (result instanceof ValueNode)
        {
            return result.getValueAsText();
        }
        if (result instanceof ArrayNode)
        {
            List<Object> parts = new ArrayList<Object>();
            for (Iterator<JsonNode> i = (result).getElements(); i.hasNext();)
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
        return "json-node";
    }
}
