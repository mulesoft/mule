/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.filters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.io.IOException;

import org.junit.Test;

public class ExpressionFilterTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testHeaderFilterEL() throws Exception
    {
        ExpressionFilter filter = new ExpressionFilter("message.outboundProperties['foo']=='bar'");
        filter.setMuleContext(muleContext);
        MuleMessage message = new DefaultMuleMessage("blah", muleContext);
        assertTrue(!filter.accept(message));
        message.setOutboundProperty("foo", "bar");
        assertTrue(filter.accept(message));
    }

    @Test
    public void testVariableFilterEL() throws Exception
    {
        ExpressionFilter filter = new ExpressionFilter("flowVars['foo']=='bar'");
        filter.setMuleContext(muleContext);
        MuleEvent event = getTestEvent("blah");
        assertTrue(!filter.accept(event));
        event.setFlowVariable("foo", "bar");
        assertTrue(filter.accept(event));
    }
    @Test
    public void testHeaderFilterWithNotEL() throws Exception
    {
        ExpressionFilter filter = new ExpressionFilter("message.outboundProperties['foo']!='bar'");
        filter.setMuleContext(muleContext);

        MuleMessage message = new DefaultMuleMessage("blah", muleContext);

        assertTrue(filter.accept(message));
        message.setOutboundProperty("foo", "bar");
        assertTrue(!filter.accept(message));
        message.setOutboundProperty("foo", "car");
        assertTrue(filter.accept(message));
    }

    @Test
    public void testVariableFilterWithNotEL() throws Exception
    {
        ExpressionFilter filter = new ExpressionFilter("flowVars['foo']!='bar'");
        filter.setMuleContext(muleContext);

        MuleEvent event = getTestEvent("blah");

        assertTrue(filter.accept(event));
        event.setFlowVariable("foo", "bar");
        assertTrue(!filter.accept(event));
        event.setFlowVariable("foo", "car");
        assertTrue(filter.accept(event));
    }

    private void removeProperty(MuleMessage message)
    {
        message.removeOutboundProperty("foo");
    }

    @Test
    public void testHeaderFilterWithNotNullEL() throws Exception
    {
        ExpressionFilter filter = new ExpressionFilter("message.outboundProperties['foo']!=null");
        filter.setMuleContext(muleContext);

        MuleMessage message = new DefaultMuleMessage("blah", muleContext);

        assertTrue(!filter.accept(message));
        removeProperty(message);
        assertTrue(!filter.accept(message));
        message.setOutboundProperty("foo", "car");
        assertTrue(filter.accept(message));
    }

    @Test
    public void testVariableFilterWithNotNullEL() throws Exception
    {
        ExpressionFilter filter = new ExpressionFilter("flowVars['foo']!=null");
        filter.setMuleContext(muleContext);

        MuleEvent event = getTestEvent("blah");

        assertTrue(!filter.accept(event));
        removeProperty(event.getMessage());
        assertTrue(!filter.accept(event));
        event.setFlowVariable("foo", "car");
        assertTrue(filter.accept(event));
    }

    @Test
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

    @Test
    public void testRegexFilterWithAngleBrackets()
    {

        ExpressionFilter filter = new ExpressionFilter("#[regex('The number is [1-9]')]");
        filter.setMuleContext(muleContext);

        assertNotNull(filter.getExpression());

        assertTrue(filter.accept(new DefaultMuleMessage("The number is 4", muleContext)));
        assertFalse(filter.accept(new DefaultMuleMessage("Say again?", muleContext)));

        assertFalse(filter.accept(new DefaultMuleMessage("The number is 0", muleContext)));
    }

    @Test
    public void testExceptionTypeFilter()
    {
        ExpressionFilter filter = new ExpressionFilter("exception is java.lang.Exception");
        filter.setMuleContext(muleContext);

        MuleMessage m = new DefaultMuleMessage("test", muleContext);
        assertTrue(!filter.accept(m));
        m.setExceptionPayload(new DefaultExceptionPayload(new IllegalArgumentException("test")));
        assertTrue(filter.accept(m));

        filter = new ExpressionFilter("exception is java.io.IOException");
        filter.setMuleContext(muleContext);
        assertTrue(!filter.accept(m));
        m.setExceptionPayload(new DefaultExceptionPayload(new IOException("test")));
        assertTrue(filter.accept(m));
    }

    @Test
    public void testExceptionTypeFilterEL()
    {
        ExpressionFilter filter = new ExpressionFilter("exception is java.lang.Exception");
        filter.setMuleContext(muleContext);

        MuleMessage m = new DefaultMuleMessage("test", muleContext);
        assertTrue(!filter.accept(m));
        m.setExceptionPayload(new DefaultExceptionPayload(new IllegalArgumentException("test")));
        assertTrue(filter.accept(m));

        filter = new ExpressionFilter("exception is java.io.IOException");
        filter.setMuleContext(muleContext);
        assertTrue(!filter.accept(m));
        m.setExceptionPayload(new DefaultExceptionPayload(new IOException("test")));
        assertTrue(filter.accept(m));
    }

    @Test
    public void testPayloadTypeFilterEL()
    {
        ExpressionFilter filter = new ExpressionFilter("payload is org.mule.tck.testmodels.fruit.Apple");
        filter.setMuleContext(muleContext);

        assertTrue(filter.accept(new DefaultMuleMessage(new Apple(), muleContext)));
        assertTrue(!filter.accept(new DefaultMuleMessage("test", muleContext)));

        filter = new ExpressionFilter("payload is String");
        filter.setMuleContext(muleContext);
        assertTrue(filter.accept(new DefaultMuleMessage("test", muleContext)));
        assertTrue(!filter.accept(new DefaultMuleMessage(new Exception("test"), muleContext)));
    }

    @Test
    public void testTrueStringEL()
    {
        ExpressionFilter filter = new ExpressionFilter("payload");
        filter.setMuleContext(muleContext);

        filter.setNullReturnsTrue(true);

        assertTrue(filter.accept(new DefaultMuleMessage("true", muleContext)));
        assertTrue(filter.accept(new DefaultMuleMessage("TRUE", muleContext)));
        assertTrue(filter.accept(new DefaultMuleMessage("tRuE", muleContext)));
    }

    @Test
    public void testFalseStringEL()
    {
        ExpressionFilter filter = new ExpressionFilter("payload");
        filter.setMuleContext(muleContext);

        filter.setNullReturnsTrue(false);

        assertFalse(filter.accept(new DefaultMuleMessage("false", muleContext)));
        assertFalse(filter.accept(new DefaultMuleMessage("FALSE", muleContext)));
        assertFalse(filter.accept(new DefaultMuleMessage("faLSe", muleContext)));
    }

}
