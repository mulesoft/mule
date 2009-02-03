package org.mule.expression;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.expression.RequiredValueException;
import org.mule.tck.AbstractMuleTestCase;

import java.util.HashMap;
import java.util.Map;

public class MapPayloadExpressionEvaluatorTestCase extends AbstractMuleTestCase
{

    private Map<String, String> props = new HashMap<String, String>(3);

    @Override
    public void doSetUp()
    {
        props.clear();
        props.put("foo", "moo");
        props.put("bar", "mar");
        props.put("ba*z", "maz");
    }

    public void testExpressions() throws Exception
    {
        MapPayloadExpressionEvaluator eval = new MapPayloadExpressionEvaluator();
        MuleMessage message = new DefaultMuleMessage(props, (Map) null);

        // direct match
        Object result = eval.evaluate("foo", message);
        assertEquals("moo", result);

        // direct match, optional
        result = eval.evaluate("bar*", message);
        assertEquals("mar", result);

        // direct match with * inline
        result = eval.evaluate("ba*z", message);
        assertEquals("maz", result);

        // no match, optional
        result = eval.evaluate("fool*", message);
        assertNull(result);

        try
        {
            // no match, required
            eval.evaluate("fool", message);
            fail("Should've failed with an exception.");
        }
        catch (RequiredValueException rex)
        {
            // expected
            assertTrue(rex.getMessage().contains("fool"));
        }


    }

}
