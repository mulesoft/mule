/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    protected String getConfigResources()
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
