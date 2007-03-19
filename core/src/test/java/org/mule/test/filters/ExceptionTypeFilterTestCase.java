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

import java.io.IOException;

import org.mule.impl.MuleMessage;
import org.mule.impl.message.ExceptionPayload;
import org.mule.routing.filters.ExceptionTypeFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOMessage;

public class ExceptionTypeFilterTestCase extends AbstractMuleTestCase
{

    public void testExceptionTypeFilter()
    {
        ExceptionTypeFilter filter = new ExceptionTypeFilter();
        assertNull(filter.getExpectedType());
        UMOMessage m = new MuleMessage("test");
        assertTrue(!filter.accept(m));
        m.setExceptionPayload(new ExceptionPayload(new NullPointerException("test")));
        assertTrue(filter.accept(m));

        filter = new ExceptionTypeFilter(IOException.class);
        assertTrue(!filter.accept(m));
        m.setExceptionPayload(new ExceptionPayload(new IOException("test")));
        assertTrue(filter.accept(m));
    }

}
