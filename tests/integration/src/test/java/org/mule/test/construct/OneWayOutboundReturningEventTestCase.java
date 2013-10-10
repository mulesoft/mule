/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.construct;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleMessage;

public class OneWayOutboundReturningEventTestCase extends OneWayOutboundTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/construct/one-way-outbound-config.xml";
    }

    @Override
    protected void assertOneWayOutboundResponse(MuleMessage response)
    {
        assertEquals("TEST", response.getPayload());
    }

    @Override
    protected void assertOneWayOutboundAfterComponentResponse(MuleMessage response)
    {
        assertEquals("TEST processed", response.getPayload());
    }

}
