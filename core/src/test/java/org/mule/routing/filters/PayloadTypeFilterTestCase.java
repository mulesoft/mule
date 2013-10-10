/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
