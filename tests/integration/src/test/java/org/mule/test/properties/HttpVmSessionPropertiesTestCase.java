/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.properties;

import static org.junit.Assert.assertThat;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Collections;
import java.util.Map;

import org.hamcrest.core.IsNull;
import org.junit.Rule;
import org.junit.Test;

public class HttpVmSessionPropertiesTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/properties/session-properties-http-vm-config.xml";
    }

    /**
     * Test that the Session property are copied correctly from Http flow to VM flows transport
     * correctly copies outbound to inbound property both for requests amd responses
     */
    @Test
    public void testPropertiesFromHttpToVm() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Object> properties = Collections.emptyMap();
        MuleMessage message = client.send("http://localhost:" + dynamicPort1.getNumber() + "/http-inbound-flow", new DefaultMuleMessage("some message", properties, muleContext));
        assertThat(message, IsNull.<Object>notNullValue());
        assertThat(message.getExceptionPayload(), IsNull.<Object>nullValue());
    }

    /**
     * Test that the Session property are copied correctly from VM flow to Http flows transport correctly copies outbound to inbound property both for requests amd responses
     */
    @Test
    public void testPropertiesFromVmToHttp() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Object> properties = Collections.emptyMap();
        MuleMessage message = client.send("vm://vm-inbound-flow", new DefaultMuleMessage("some message", properties, muleContext));
        assertThat(message, IsNull.<Object>notNullValue());
        assertThat(message.getExceptionPayload(), IsNull.<Object>nullValue());
    }
}
