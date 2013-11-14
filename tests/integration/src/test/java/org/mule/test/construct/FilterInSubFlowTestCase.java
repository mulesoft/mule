/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class FilterInSubFlowTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/construct/sub-flow-with-filter-config.xml";
    }

    @Test
    public void filterOnFlow() throws MuleException
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://testFlow", "TEST", null);
        assertEquals(null, response);
    }

    @Test
    public void filterOnSubFlow() throws MuleException
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://testSubFlow", "TEST", null);
        assertEquals(null, response);
    }
}
