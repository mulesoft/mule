/*
 * $Id: WireTapTestCase.java 17050 2010-04-20 02:52:45Z dfeist $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.routing.filter.Filter;
import org.mule.routing.filters.EqualsFilter;
import org.mule.routing.outbound.RouterTestUtils;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;


public class WireTapTestCase extends AbstractMuleTestCase
{
    public WireTapTestCase()
    {
        setStartContext(true);
    }

    public void testWireTap() throws Exception
    {
        Filter payloadIsOK = new EqualsFilter("OK");
        Filter payloadIsBad = new EqualsFilter("Bad");
        MuleMessage okMessage = new DefaultMuleMessage("OK", muleContext);
        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider", "test://Test1Provider?synchronous=false");
        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Provider", "test://Test2Provider?synchronous=false");
        Mock tap = RouterTestUtils.getMockEndpoint(endpoint2);
        Mock session = MuleTestUtils.getMockSession();
        session.expectAndReturn("getFlowConstruct", null);
        session.expect("setFlowConstruct", C.ANY_ARGS);
        MuleEvent event = new DefaultMuleEvent(okMessage, endpoint1, (MuleSession) session.proxy());

        WireTapMessageProcessor wireTap = new WireTapMessageProcessor();


        // No processing if no tap endpoint is specified
        wireTap.process(event);
        tap.verify();

        // No processing if filter doesn't match
        wireTap.setTap((OutboundEndpoint)tap.proxy());
        wireTap.setFilter(payloadIsBad);
        event = new DefaultMuleEvent(okMessage, endpoint1, (MuleSession) session.proxy());
        wireTap.process(event);
        tap.verify();

        // This one will process the event on the tap's endpoint
        wireTap.setFilter(payloadIsOK);
        tap.expect("process", C.isA(MuleEvent.class));
        wireTap.process(event);
        tap.verify();

        // As will this, since null filters match everything
        wireTap.setFilter(null);
        tap.expect("process", C.isA(MuleEvent.class));
        wireTap.process(event);
        tap.verify();
    }

}