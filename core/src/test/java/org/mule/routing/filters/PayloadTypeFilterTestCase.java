/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.filters;

import org.mule.DefaultMuleMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PayloadTypeFilterTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testPayloadTypeFilterNoExpectedType()
    {
        PayloadTypeFilter filter = new PayloadTypeFilter();
        assertNull(filter.getExpectedType());
        assertFalse(filter.accept(new DefaultMuleMessage("test", muleContext)));

        filter.setExpectedType(String.class);
        assertTrue(filter.accept(new DefaultMuleMessage("test", muleContext)));

        filter.setExpectedType(null);
        assertFalse(filter.accept(new DefaultMuleMessage("test", muleContext)));
    }

    @Test
    public void testPayloadTypeFilter()
    {
        PayloadTypeFilter filter = new PayloadTypeFilter(Exception.class);
        assertNotNull(filter.getExpectedType());
        assertTrue(filter.accept(new DefaultMuleMessage(new Exception("test"), muleContext)));
        assertTrue(!filter.accept(new DefaultMuleMessage("test", muleContext)));

        filter.setExpectedType(String.class);
        assertTrue(filter.accept(new DefaultMuleMessage("test", muleContext)));
        assertTrue(!filter.accept(new DefaultMuleMessage(new Exception("test"), muleContext)));
    }

}
