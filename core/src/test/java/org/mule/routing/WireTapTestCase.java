/*
 * $Id: WireTapTestCase.java 17050 2010-04-20 02:52:45Z dfeist $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.SensingNullMessageProcessor;

public class WireTapTestCase extends AbstractMuleTestCase
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

    public void testWireTapNoFilter() throws Exception
    {
        MuleEvent event = getTestInboundEvent("data");
        MuleEvent primaryOutput = wireTap.process(event);

        assertSame(event, primaryOutput);

        assertNotNull(tapListener.event);
        assertEquals(event.getMessage().getPayload(), tapListener.event.getMessage().getPayload());
    }

    public void testWireTapFilterAccepted() throws Exception
    {
        wireTap.setFilter(new Filter()
        {
            public boolean accept(MuleMessage message)
            {
                return true;
            }
        });

        MuleEvent event = getTestInboundEvent("data");
        MuleEvent primaryOutput = wireTap.process(event);

        assertSame(event, primaryOutput);

        assertNotNull(tapListener.event);
        assertEquals(event.getMessage().getPayload(), tapListener.event.getMessage().getPayload());
    }

    public void testWireTapFilterUnaccepted() throws Exception
    {
        wireTap.setFilter(new Filter()
        {
            public boolean accept(MuleMessage message)
            {
                return false;
            }
        });

        MuleEvent event = getTestInboundEvent("data");
        MuleEvent primaryOutput = wireTap.process(event);

        assertSame(event, primaryOutput);

        assertNull(tapListener.event);
    }

    public void testWireTapNullTap() throws Exception
    {
        wireTap.setTap(null);

        MuleEvent event = getTestInboundEvent("data");
        MuleEvent primaryOutput = wireTap.process(event);

        assertSame(event, primaryOutput);
    }

}
