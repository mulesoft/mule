/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.filters;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.message.DefaultExceptionPayload;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class ExceptionTypeFilterTestCase extends AbstractMuleTestCase
{

    private MuleContext muleContext = mock(MuleContext.class);

    @Test
    public void testExceptionTypeFilter()
    {
        ExceptionTypeFilter filter = new ExceptionTypeFilter();
        assertNull(filter.getExpectedType());
        MuleMessage m = new DefaultMuleMessage("test", muleContext);
        assertTrue(!filter.accept(m));
        m.setExceptionPayload(new DefaultExceptionPayload(new IllegalArgumentException("test")));
        assertTrue(filter.accept(m));

        filter = new ExceptionTypeFilter(IOException.class);
        assertTrue(!filter.accept(m));
        m.setExceptionPayload(new DefaultExceptionPayload(new IOException("test")));
        assertTrue(filter.accept(m));
    }

}
