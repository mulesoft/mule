/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;

import static org.junit.Assert.assertEquals;

import org.mule.module.scripting.expression.GroovyExpressionEvaluator;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.jdbc.JdbcConnector;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ComplexExpressionParsingTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testComplexExpressionJdbcParsing() throws Exception
    {
        muleContext.getExpressionManager().registerEvaluator(new GroovyExpressionEvaluator());

        List<String> parsedParams = new ArrayList<String>();
        JdbcConnector c = new JdbcConnector(muleContext);
        String result = c.parseStatement("#[groovy:payload[0]] - #[groovy:payload[1].toUpperCase()]", parsedParams);

        assertEquals("Wrong number of parsed parameters for a statement", 2, parsedParams.size());
        assertEquals("#[groovy:payload[0]]", parsedParams.get(0));
        assertEquals("#[groovy:payload[1].toUpperCase()]", parsedParams.get(1));

        // it's jdbc's PreparedStatement params here
        assertEquals("? - ?", result);
    }
}
