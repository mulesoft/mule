/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
    protected String getConfigFile()
    {
        return "org/mule/properties/session-properties-in-exception-strategy-config.xml";
    }

    @Test
    public void sessionPropertyIsNotLost() throws Exception
    {
        MuleClient client = muleContext.getClient();

        List<String> list = new ArrayList<String>();
        list.add("one");
        list.add("two");
        list.add("three");

        MuleMessage result = client.send("vm://testInput", list, null);

        assertNull(result.getExceptionPayload());
        assertFalse(result.getPayload() instanceof NullPayload);
        assertEquals(list.size(), result.getSessionProperty("ErrorCount"));
    }
}
