/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test;

import org.mule.module.scripting.expression.GroovyExpressionEvaluator;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.jdbc.JdbcConnector;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ComplexExpressionParsingTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testComplexExpressionJdbcParsing() throws Exception
    {
        muleContext.getExpressionManager().registerEvaluator(new GroovyExpressionEvaluator());

        List parsedParams = new ArrayList();
        JdbcConnector c = new JdbcConnector(muleContext);
        String result = c.parseStatement("#[groovy:payload[0]] - #[groovy:payload[1].toUpperCase()]", parsedParams);

        assertEquals("Wrong number of parsed parameters for a statement", 2, parsedParams.size());
        assertEquals("#[groovy:payload[0]]", parsedParams.get(0));
        assertEquals("#[groovy:payload[1].toUpperCase()]", parsedParams.get(1));

        // it's jdbc's PreparedStatement params here
        assertEquals("? - ?", result);
    }

}
