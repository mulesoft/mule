/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.filters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

public class ConsumableFilterTestCase extends AbstractMuleTestCase
{

    private ConsumableMuleMessageFilter filter;

    @Before
    public void setUp() throws Exception
    {
        filter = new ConsumableMuleMessageFilter();
    }

    @Test
    public void testRejectsNonDefaultMuleMessageInstances() throws Exception
    {
        MuleMessage message = mock(MuleMessage.class);
        assertFalse("Should reject non DefaultMuleMessage instances", filter.accept(message));
    }

    @Test
    public void testRejectsConsumablePayload() throws Exception
    {

        MuleContext context = mock(MuleContext.class, RETURNS_DEEP_STUBS);
        InputStream is = new ByteArrayInputStream("TEST".getBytes());
        MuleMessage message = new DefaultMuleMessage(is, context);
        assertFalse("Should reject consumable payload", filter.accept(message));
    }

    @Test
    public void testAcceptsNonConsumablePayload() throws Exception
    {
        MuleContext context = mock(MuleContext.class, RETURNS_DEEP_STUBS);
        MuleMessage message = new DefaultMuleMessage("TEST", context);
        assertTrue("Should accept non consumable payload", filter.accept(message));
    }
}
