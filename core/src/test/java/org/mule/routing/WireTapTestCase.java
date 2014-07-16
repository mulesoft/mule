/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class WireTapTestCase extends AbstractMuleContextTestCase
{

    protected SensingNullMessageProcessor tapListener;
    protected WireTap wireTap;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        wireTap = new WireTap();
        tapListener = getSensingNullMessageProcessor();
        wireTap.setTap(tapListener);
    }

    @Test
    public void testWireTapNoFilter() throws Exception
    {
        MuleEvent event = getTestEvent("data");
        MuleEvent primaryOutput = wireTap.process(event);

        assertSame(event, primaryOutput);

        assertNotNull(tapListener.event);
        assertEquals(event.getMessage().getPayload(), tapListener.event.getMessage().getPayload());
    }

    @Test
    public void testWireTapFilterAccepted() throws Exception
    {
        wireTap.setFilter(new Filter()
        {
            public boolean accept(MuleMessage message)
            {
                return true;
            }
        });

        MuleEvent event = getTestEvent("data");
        MuleEvent primaryOutput = wireTap.process(event);

        assertSame(event, primaryOutput);

        assertNotNull(tapListener.event);
        assertEquals(event.getMessage().getPayload(), tapListener.event.getMessage().getPayload());
    }

    @Test
    public void testWireTapFilterUnaccepted() throws Exception
    {
        wireTap.setFilter(new Filter()
        {
            public boolean accept(MuleMessage message)
            {
                return false;
            }
        });

        MuleEvent event = getTestEvent("data");
        MuleEvent primaryOutput = wireTap.process(event);

        assertSame(event, primaryOutput);

        assertNull(tapListener.event);
    }

    @Test
    public void testWireTapNullTap() throws Exception
    {
        wireTap.setTap(null);

        MuleEvent event = getTestEvent("data");
        MuleEvent primaryOutput = wireTap.process(event);

        assertSame(event, primaryOutput);
    }

}
