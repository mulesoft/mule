/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class SessionPropertiesInExceptionStrategyTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/properties/session-properties-in-exception-strategy-config.xml";
    }

    @Test
    public void sessionPropertyIsNotLost() throws Exception
    {
        MuleClient client = muleContext.getClient();

        List list = new ArrayList<String>();
        list.add("one");
        list.add("two");
        list.add("three");

        MuleMessage result = client.send("vm://testInput", list, null);

        assertNull(result.getExceptionPayload());
        assertFalse(result.getPayload() instanceof NullPayload);
        assertEquals(list.size(), result.getSessionProperty("ErrorCount"));
    }
}
