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
import org.mule.tck.AbstractMuleTestCase;

public class PayloadTypeFilterTestCase extends AbstractMuleTestCase
{

    public void testPayloadTypeFilterNoExpectedType()
    {
        PayloadTypeFilter filter = new PayloadTypeFilter();
        assertNull(filter.getExpectedType());
        assertFalse(filter.accept(new DefaultMuleMessage("test")));

        filter.setExpectedType(String.class);
        assertTrue(filter.accept(new DefaultMuleMessage("test")));

        filter.setExpectedType(null);
        assertFalse(filter.accept(new DefaultMuleMessage("test")));
    }

    public void testPayloadTypeFilter()
    {
        PayloadTypeFilter filter = new PayloadTypeFilter(Exception.class);
        assertNotNull(filter.getExpectedType());
        assertTrue(filter.accept(new DefaultMuleMessage(new Exception("test"))));
        assertTrue(!filter.accept(new DefaultMuleMessage("test")));

        filter.setExpectedType(String.class);
        assertTrue(filter.accept(new DefaultMuleMessage("test")));
        assertTrue(!filter.accept(new DefaultMuleMessage(new Exception("test"))));
    }

}
