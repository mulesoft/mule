/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JsonNodeExpressionEvaluatorTestCase extends JsonExpressionEvaluatorTestCase
{

    @Override
    protected JsonExpressionEvaluator getEvaluator()
    {
        return new JsonNodeExpressionEvaluator();
    }

    @Override
    protected String getEvaluatorName()
    {
        return "json-node";
    }

    @Test
    public void testReturnTypes()
    {
        // String
        assertEquals(String.class, eval.evaluate("[0]/user/name", message).getClass());
        // Number
        assertEquals(String.class, eval.evaluate("[0]/id", message).getClass());
        // Boolean
        assertEquals(String.class, eval.evaluate("[0]/truncated", message).getClass());
        // Object
        assertEquals(ObjectNode.class, eval.evaluate("[0]/user", message).getClass());
        // Array
        assertEquals(ArrayList.class, eval.evaluate("[0]/anArray", message).getClass());
        assertEquals(String.class, ((List) eval.evaluate("[0]/anArray", message)).get(0).getClass());
        assertEquals(ObjectNode.class, ((List) eval.evaluate("[0]/anArray", message)).get(1).getClass());
        assertEquals(ArrayList.class, ((List) eval.evaluate("[0]/anArray", message)).get(2).getClass());
    }

}
