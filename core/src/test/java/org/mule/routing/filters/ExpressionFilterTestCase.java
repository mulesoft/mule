/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.filters;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.message.DefaultExceptionPayload;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.io.IOException;

public class ExpressionFilterTestCase extends AbstractMuleTestCase
{

    public void testHeaderFilter() throws Exception
    {
        ExpressionFilter filter = new ExpressionFilter("header", "foo=bar");
        filter.setMuleContext(muleContext);
        MuleMessage message = new DefaultMuleMessage("blah");
        assertTrue(!filter.accept(message));
        message.setProperty("foo", "bar");
        assertTrue(filter.accept(message));
    }

    public void testHeaderFilterWithNot() throws Exception
    {
        ExpressionFilter filter = new ExpressionFilter("header", "foo!=bar");
        filter.setMuleContext(muleContext);

        MuleMessage message = new DefaultMuleMessage("blah");

        assertTrue(filter.accept(message));
        message.setProperty("foo", "bar");
        assertTrue(!filter.accept(message));
        message.setProperty("foo", "car");
        assertTrue(filter.accept(message));
    }

    public void testHeaderFilterWithNotNull() throws Exception
    {
        ExpressionFilter filter = new ExpressionFilter("header", "foo!=null");
        filter.setMuleContext(muleContext);

        MuleMessage message = new DefaultMuleMessage("blah");

        assertTrue(!filter.accept(message));
        message.removeProperty("foo");
        assertTrue(!filter.accept(message));
        message.setProperty("foo", "car");
        assertTrue(filter.accept(message));
    }

    public void testRegexFilterNoPattern()
    {
        // start with default
        RegExFilter filter = new RegExFilter();
        assertNull(filter.getPattern());
        assertFalse(filter.accept("No tengo dinero"));

        // activate a pattern
        filter.setPattern("(.*) brown fox");
        assertTrue(filter.accept("The quick brown fox"));

        // remove pattern again, i.e. block all
        filter.setPattern(null);
        assertFalse(filter.accept("oh-oh"));
    }

    public void testRegexFilter()
    {

        ExpressionFilter filter = new ExpressionFilter("regex", "The quick (.*)");
        filter.setMuleContext(muleContext);

        assertNotNull(filter.getExpression());

        assertTrue(filter.accept(new DefaultMuleMessage("The quick brown fox")));
        assertTrue(filter.accept(new DefaultMuleMessage("The quick ")));

        assertTrue(!filter.accept(new DefaultMuleMessage("The quickbrown fox")));
        assertTrue(!filter.accept(new DefaultMuleMessage("he quick brown fox")));
    }

    public void testExceptionTypeFilter()
    {
        ExpressionFilter filter = new ExpressionFilter("exception-type:java.lang.Exception");
        filter.setMuleContext(muleContext);

        MuleMessage m = new DefaultMuleMessage("test");
        assertTrue(!filter.accept(m));
        m.setExceptionPayload(new DefaultExceptionPayload(new IllegalArgumentException("test")));
        assertTrue(filter.accept(m));

        filter = new ExpressionFilter("exception-type:java.io.IOException");
        assertTrue(!filter.accept(m));
        m.setExceptionPayload(new DefaultExceptionPayload(new IOException("test")));
        assertTrue(filter.accept(m));
    }

    public void testPayloadTypeFilter()
    {
        ExpressionFilter filter = new ExpressionFilter("payload-type:org.mule.tck.testmodels.fruit.Apple");
        filter.setMuleContext(muleContext);

        assertTrue(filter.accept(new DefaultMuleMessage(new Apple())));
        assertTrue(!filter.accept(new DefaultMuleMessage("test")));

        filter = new ExpressionFilter("payload-type:java.lang.String");
        assertTrue(filter.accept(new DefaultMuleMessage("test")));
        assertTrue(!filter.accept(new DefaultMuleMessage(new Exception("test"))));
    }

    public void testWildcardFilterMultiplePatterns()
    {
        ExpressionFilter filter = new ExpressionFilter("wildcard:* brown*, The*");
        filter.setMuleContext(muleContext);

        assertTrue(filter.accept(new DefaultMuleMessage("The quick brown fox")));
        assertTrue(filter.accept(new DefaultMuleMessage(" brown fox")));
        assertTrue(filter.accept(new DefaultMuleMessage("The quickbrown fox")));
    }

    public void testTrueString()
    {
        ExpressionFilter filter = new ExpressionFilter("payload:");
        filter.setMuleContext(muleContext);

        filter.setNullReturnsTrue(true);

        assertTrue(filter.accept(new DefaultMuleMessage("true")));
        assertTrue(filter.accept(new DefaultMuleMessage("TRUE")));
        assertTrue(filter.accept(new DefaultMuleMessage("tRuE")));
    }

    public void testFalseString()
    {
        ExpressionFilter filter = new ExpressionFilter("payload:");
        filter.setMuleContext(muleContext);

        filter.setNullReturnsTrue(false);

        assertFalse(filter.accept(new DefaultMuleMessage("false")));
        assertFalse(filter.accept(new DefaultMuleMessage("FALSE")));
        assertFalse(filter.accept(new DefaultMuleMessage("faLSe")));
    }

    public void testOtherString()
    {
        ExpressionFilter filter = new ExpressionFilter("payload:");
        filter.setMuleContext(muleContext);
        
        filter.setNullReturnsTrue(false);

        assertFalse(filter.accept(new DefaultMuleMessage("otherTrueString")));
        assertFalse(filter.accept(new DefaultMuleMessage("otherFalseString")));
        assertFalse(filter.accept(new DefaultMuleMessage("!trueFALSE")));
    }
}
