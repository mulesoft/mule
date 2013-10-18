/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.DefaultMuleMessage;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.HashMap;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExpressionParsingTestCase extends AbstractMuleContextTestCase
{

    private ExpressionEvaluator newlyRegistered;

    @Override
    protected void doSetUp() throws Exception
    {
        newlyRegistered = new MapPayloadExpressionEvaluator();
        String name = newlyRegistered.getName();
        if (muleContext.getExpressionManager().isEvaluatorRegistered(name))
        {
            newlyRegistered = null;
        }
        else
        {
            muleContext.getExpressionManager().registerEvaluator(newlyRegistered);
        }
    }

    @Override
    protected void doTearDown() throws Exception
    {
        if (newlyRegistered == null)
        {
            return;
        }
        String name = newlyRegistered.getName();
        muleContext.getExpressionManager().unregisterEvaluator(name);
    }

    @Test
    public void testEvaluatorBraces()
    {
        String template = "#[map-payload:time] - #[map-payload:comment]";

        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("time", "12:10");
        map.put("comment", "$3 vs $3 shinogi 41+51.t must be #1140ob");

        assertEquals(
                "12:10 - comment", "12:10 - $3 vs $3 shinogi 41+51.t must be #1140ob",
                muleContext.getExpressionManager().parse(template, new DefaultMuleMessage(map, muleContext)));
    }
}

