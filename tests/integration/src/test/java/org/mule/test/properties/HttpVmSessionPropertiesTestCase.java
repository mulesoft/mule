/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.properties;

import org.hamcrest.core.IsNull;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;
import org.mule.module.client.MuleClient;
import org.mule.tck.DynamicPortTestCase;
import org.mule.tck.FunctionalTestCase;

import java.util.Collections;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class HttpVmSessionPropertiesTestCase extends DynamicPortTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/properties/session-properties-http-vm-config.xml";
    }

    /**
     * Test that the Session property are copied correctly from Http flow to VM flows transport
     * correctly copies outbound to inbound property both for requests amd responses
     */
    public void testPropertiesFromHttpToVm() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("http://localhost:" + getPorts().get(0) + "/http-inbound-flow", "some message", Collections.emptyMap());
        assertThat(message, IsNull.<Object>notNullValue());
        assertThat(message.getExceptionPayload(), IsNull.<Object>nullValue());
    }

    /**
     * Test that the Session property are copied correctly from VM flow to Http flows transport correctly copies outbound to inbound property both for requests amd responses
     */
    public void testPropertiesFromVmToHttp() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://vm-inbound-flow", "some message", Collections.emptyMap());
        assertThat(message, IsNull.<Object>notNullValue());
        assertThat(message.getExceptionPayload(), IsNull.<Object>nullValue());
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 2;
    }
}
