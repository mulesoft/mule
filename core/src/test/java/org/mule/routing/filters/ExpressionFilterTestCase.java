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
        MuleMessage message = new DefaultMuleMessage("blah", muleContext);
        assertTrue(!filter.accept(message));
        message.setProperty("foo", "bar");
        assertTrue(filter.accept(message));
    }

    public void testHeaderFilterWithNot() throws Exception
    {
        ExpressionFilter filter = new ExpressionFilter("header", "foo!=bar");
        filter.setMuleContext(muleContext);

        MuleMessage message = new DefaultMuleMessage("blah", muleContext);

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

        MuleMessage message = new DefaultMuleMessage("blah", muleContext);

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

        assertTrue(filter.accept(new DefaultMuleMessage("The quick brown fox", muleContext)));
        assertTrue(filter.accept(new DefaultMuleMessage("The quick ", muleContext)));

        assertTrue(!filter.accept(new DefaultMuleMessage("The quickbrown fox", muleContext)));
        assertTrue(!filter.accept(new DefaultMuleMessage("he quick brown fox", muleContext)));
    }

    public void testExceptionTypeFilter()
    {
        ExpressionFilter filter = new ExpressionFilter("exception-type:java.lang.Exception");
        filter.setMuleContext(muleContext);

        MuleMessage m = new DefaultMuleMessage("test", muleContext);
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

        assertTrue(filter.accept(new DefaultMuleMessage(new Apple(), muleContext)));
        assertTrue(!filter.accept(new DefaultMuleMessage("test", muleContext)));

        filter = new ExpressionFilter("payload-type:java.lang.String");
        assertTrue(filter.accept(new DefaultMuleMessage("test", muleContext)));
        assertTrue(!filter.accept(new DefaultMuleMessage(new Exception("test"), muleContext)));
    }

    public void testWildcardFilterMultiplePatterns()
    {
        ExpressionFilter filter = new ExpressionFilter("wildcard:* brown*, The*");
        filter.setMuleContext(muleContext);

        assertTrue(filter.accept(new DefaultMuleMessage("The quick brown fox", muleContext)));
        assertTrue(filter.accept(new DefaultMuleMessage(" brown fox", muleContext)));
        assertTrue(filter.accept(new DefaultMuleMessage("The quickbrown fox", muleContext)));
    }

    public void testTrueString()
    {
        ExpressionFilter filter = new ExpressionFilter("payload:");
        filter.setMuleContext(muleContext);

        filter.setNullReturnsTrue(true);

        assertTrue(filter.accept(new DefaultMuleMessage("true", muleContext)));
        assertTrue(filter.accept(new DefaultMuleMessage("TRUE", muleContext)));
        assertTrue(filter.accept(new DefaultMuleMessage("tRuE", muleContext)));
    }

    public void testFalseString()
    {
        ExpressionFilter filter = new ExpressionFilter("payload:");
        filter.setMuleContext(muleContext);

        filter.setNullReturnsTrue(false);

        assertFalse(filter.accept(new DefaultMuleMessage("false", muleContext)));
        assertFalse(filter.accept(new DefaultMuleMessage("FALSE", muleContext)));
        assertFalse(filter.accept(new DefaultMuleMessage("faLSe", muleContext)));
    }

    public void testOtherString()
    {
        ExpressionFilter filter = new ExpressionFilter("payload:");
        filter.setMuleContext(muleContext);
        
        filter.setNullReturnsTrue(false);

        assertFalse(filter.accept(new DefaultMuleMessage("otherTrueString", muleContext)));
        assertFalse(filter.accept(new DefaultMuleMessage("otherFalseString", muleContext)));
        assertFalse(filter.accept(new DefaultMuleMessage("!trueFALSE", muleContext)));
    }
}
