/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.filters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.routing.filters.ConsumableMuleMessageFilter;
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

        MuleContext context = mock(MuleContext.class);
        InputStream is = new ByteArrayInputStream("TEST".getBytes());
        MuleMessage message = new DefaultMuleMessage(is, context);
        assertFalse("Should reject consumable payload", filter.accept(message));
    }

    @Test
    public void testAcceptsNonConsumablePayload() throws Exception
    {
        MuleContext context = mock(MuleContext.class);
        MuleMessage message = new DefaultMuleMessage("TEST", context);
        assertTrue("Should accept non consumable payload", filter.accept(message));
    }
}
