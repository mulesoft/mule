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
import org.mule.tck.AbstractMuleTestCase;

public class MessagePropertyFilterTestCase extends AbstractMuleTestCase
{
    public void testMessagePropertyFilter() throws Exception
    {
        MessagePropertyFilter filter = new MessagePropertyFilter("foo=bar");
        MuleMessage message = new DefaultMuleMessage("blah", muleContext);
        assertTrue(!filter.accept(message));
        message.setProperty("foo", "bar");
        assertTrue(filter.accept(message));
    }

    public void testMessagePropertyFilterWithNot() throws Exception
    {
        MessagePropertyFilter filter = new MessagePropertyFilter("foo!=bar");
        MuleMessage message = new DefaultMuleMessage("blah", muleContext);

        assertTrue(filter.accept(message));
        message.setProperty("foo", "bar");
        assertTrue(!filter.accept(message));
        message.setProperty("foo", "car");
        assertTrue(filter.accept(message));
    }

    public void testMessagePropertyFilterWithNotNull() throws Exception
    {
        MessagePropertyFilter filter = new MessagePropertyFilter("foo!=null");
        MuleMessage message = new DefaultMuleMessage("blah", muleContext);

        assertTrue(!filter.accept(message));
        message.removeProperty("foo");
        assertTrue(!filter.accept(message));
        message.setProperty("foo", "car");
        assertTrue(filter.accept(message));
    }

    public void testMessagePropertyFilterWithCaseSensitivity() throws Exception
    {
        MessagePropertyFilter filter = new MessagePropertyFilter("foo=Bar");
        MuleMessage message = new DefaultMuleMessage("blah", muleContext);
        message.setProperty("foo", "bar");
        assertTrue(!filter.accept(message));
        filter.setCaseSensitive(false);
        assertTrue(filter.accept(message));
    }


    public void testMessagePropertyFilterWithWildcard() throws Exception
    {
        MessagePropertyFilter filter = new MessagePropertyFilter("foo=B*");
        MuleMessage message = new DefaultMuleMessage("blah", muleContext);
        message.setProperty("foo", "bar");
        assertTrue(!filter.accept(message));
        filter.setCaseSensitive(false);
        assertTrue(filter.accept(message));
        filter.setPattern("foo=*a*");
        assertTrue(filter.accept(message));
    }

    public void testMessagePropertyFilterDodgyValues() throws Exception
    {
        MessagePropertyFilter filter = new MessagePropertyFilter();
        assertFalse(filter.accept(null));

        filter = new MessagePropertyFilter("foo = bar");
        MuleMessage message = new DefaultMuleMessage("blah", muleContext);
        message.setProperty("foo", "bar");
        assertTrue(filter.accept(message));
        filter.setCaseSensitive(false);

        filter = new MessagePropertyFilter("foo2 =null");
        message.removeProperty("foo2");
        assertTrue(filter.accept(message));

        filter = new MessagePropertyFilter("foo2 =");
        message.setProperty("foo2", "");
        assertTrue(filter.accept(message));

        message.removeProperty("foo2");
        assertTrue(!filter.accept(message));
    }


    public void testMessagePropertyFilterPropertyExists() throws Exception
    {
        MessagePropertyFilter filter = new MessagePropertyFilter("foo!=null");
        MuleMessage message = new DefaultMuleMessage("blah", muleContext);

        assertTrue(!filter.accept(message));
        message.setProperty("foo", "car");
        assertTrue(filter.accept(message));
    }
}
