/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleMessage;

public class OneWayOutboundReturningEventTestCase extends OneWayOutboundTestCase
{
    @Override
    protected String getConfigFile()
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
