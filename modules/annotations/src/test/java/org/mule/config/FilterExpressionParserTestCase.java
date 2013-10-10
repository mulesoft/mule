/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.mule.config.expression.ExpressionFilterParser;
import org.mule.routing.filters.ExpressionFilter;
import org.mule.routing.filters.logic.AndFilter;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FilterExpressionParserTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testSimpleFilters() throws Exception
    {
        ExpressionFilterParser parser = new ExpressionFilterParser();
        Filter f = parser.parseFilterString("#[regex:foo bar]");
        assertNotNull(f);
        assertTrue(f instanceof ExpressionFilter);

        MuleMessage message = new DefaultMuleMessage("foo bar baz", muleContext);
        assertTrue(f.accept(message));


        f = parser.parseFilterString("#[regex:.* bar] AND #[wildcard:foo*]");
        assertNotNull(f);
        assertTrue(f instanceof AndFilter);
        assertEquals(2, ((AndFilter)f).getFilters().size());
        assertTrue(((AndFilter)f).getFilters().get(0) instanceof ExpressionFilter);
        assertEquals("regex", ((ExpressionFilter)((AndFilter)f).getFilters().get(0)).getEvaluator());
        assertTrue(((AndFilter)f).getFilters().get(1) instanceof ExpressionFilter);
        assertEquals("wildcard", ((ExpressionFilter)((AndFilter)f).getFilters().get(1)).getEvaluator());

        message = new DefaultMuleMessage("foo bar", muleContext);
        assertTrue(f.accept(message));

        message = new DefaultMuleMessage("foo car", muleContext);
        assertFalse(f.accept(message));

        f = parser.parseFilterString("#[regex:.* bar] OR #[wildcard:foo*]");
        assertNotNull(f);

        message = new DefaultMuleMessage("foo bar", muleContext);
        assertTrue(f.accept(message));

        message = new DefaultMuleMessage("foo car", muleContext);
        assertTrue(f.accept(message));
    }
}
