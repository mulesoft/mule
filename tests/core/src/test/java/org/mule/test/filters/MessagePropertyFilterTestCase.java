/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.filters;

import org.mule.impl.MuleMessage;
import org.mule.routing.filters.MessagePropertyFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOMessage;

public class MessagePropertyFilterTestCase extends AbstractMuleTestCase
{

    public void testMessagePropertyFilter() throws Exception
    {
        MessagePropertyFilter filter = new MessagePropertyFilter("foo=bar");
        UMOMessage message = new MuleMessage("blah");
        assertTrue(!filter.accept(message));
        message.setProperty("foo", "bar");
        assertTrue(filter.accept(message));
    }

    public void testMessagePropertyFilterWithNot() throws Exception
    {
        MessagePropertyFilter filter = new MessagePropertyFilter("foo!=bar");
        UMOMessage message = new MuleMessage("blah");

        assertTrue(filter.accept(message));
        message.setProperty("foo", "bar");
        assertTrue(!filter.accept(message));
        message.setProperty("foo", "car");
        assertTrue(filter.accept(message));
    }

    public void testMessagePropertyFilterWithNotNull() throws Exception
    {
        MessagePropertyFilter filter = new MessagePropertyFilter("foo!=null");
        UMOMessage message = new MuleMessage("blah");

        assertTrue(!filter.accept(message));
        message.setProperty("foo", null);
        assertTrue(!filter.accept(message));
        message.setProperty("foo", "car");
        assertTrue(filter.accept(message));
    }

    public void testMessagePropertyFilterWithCaseSensitivity() throws Exception
    {
        MessagePropertyFilter filter = new MessagePropertyFilter("foo=Bar");
        UMOMessage message = new MuleMessage("blah");
        message.setProperty("foo", "bar");
        assertTrue(!filter.accept(message));
        filter.setCaseSensitive(false);
        assertTrue(filter.accept(message));
    }

    public void testMessagePropertyFilterDodgyValues() throws Exception
    {
        MessagePropertyFilter filter = new MessagePropertyFilter();
        assertFalse(filter.accept(null));

        filter = new MessagePropertyFilter("foo = bar");
        UMOMessage message = new MuleMessage("blah");
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
}
