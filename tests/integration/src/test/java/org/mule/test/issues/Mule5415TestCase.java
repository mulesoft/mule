/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.issues;

import static org.junit.Assert.assertThat;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.HashMap;

import org.hamcrest.core.IsNull;
import org.junit.Rule;
import org.junit.Test;


public class Mule5415TestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/issues/mule-5415-config.xml";
    }

    @Test
    public void testFirstRequestDoesntFail() throws Exception
    {
        MuleClient client = muleContext.getClient();
        HashMap<String, Object> properties = new HashMap<String, Object>();
        properties.put("Content-Type","application/x-www-form-urlencoded");
        MuleMessage message = client.send(String.format("http://localhost:%s?param1=1&param2=3", port1.getNumber()), new DefaultMuleMessage("message", properties, muleContext));
        assertThat(message.getExceptionPayload(), IsNull.<Object>nullValue());
    }
}
