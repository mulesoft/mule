/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.filters;

import org.mule.impl.MuleMessage;
import org.mule.tck.AbstractMuleTestCase;

public class PayloadTypeFilterTestCase extends AbstractMuleTestCase
{

    public void testPayloadTypeFilterNoExpectedType()
    {
        PayloadTypeFilter filter = new PayloadTypeFilter();
        assertNull(filter.getExpectedType());
        assertFalse(filter.accept(new MuleMessage("test")));

        filter.setExpectedType(String.class);
        assertTrue(filter.accept(new MuleMessage("test")));

        filter.setExpectedType(null);
        assertFalse(filter.accept(new MuleMessage("test")));
    }

    public void testPayloadTypeFilter()
    {
        PayloadTypeFilter filter = new PayloadTypeFilter(Exception.class);
        assertNotNull(filter.getExpectedType());
        assertTrue(filter.accept(new MuleMessage(new Exception("test"))));
        assertTrue(!filter.accept(new MuleMessage("test")));

        filter.setExpectedType(String.class);
        assertTrue(filter.accept(new MuleMessage("test")));
        assertTrue(!filter.accept(new MuleMessage(new Exception("test"))));
    }

}
