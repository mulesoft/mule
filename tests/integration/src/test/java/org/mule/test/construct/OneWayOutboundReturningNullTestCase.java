/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.junit.Assert.assertNull;

import org.mule.api.MuleMessage;

public class OneWayOutboundReturningNullTestCase extends OneWayOutboundTestCase
{
    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {
            "org/mule/test/construct/activate-flow-ending-with-one-way-endpoint-returns-null-flag-config.xml",
            "org/mule/test/construct/one-way-outbound-config.xml"
        };
    }

    @Override
    protected void assertOneWayOutboundResponse(MuleMessage response)
    {
        assertNull(response);
    }

    @Override
    protected void assertOneWayOutboundAfterComponentResponse(MuleMessage response)
    {
        assertNull(response);
    }
}
