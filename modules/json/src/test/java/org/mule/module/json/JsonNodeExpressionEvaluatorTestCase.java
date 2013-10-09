/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
