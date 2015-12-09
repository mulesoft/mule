/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;


import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.module.http.api.HttpConstants.HttpStatus.ACCEPTED;
import static org.mule.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;

import org.mule.api.MuleEvent;
import org.mule.module.cxf.testmodels.AsyncService;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;

import org.junit.Rule;
import org.junit.Test;

public class OneWayOutboundTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    @Override
    protected String getConfigFile()
    {
        return "one-way-outbound-config.xml";
    }

    @Test
    public void jaxwsClientSupportsOneWayCall() throws Exception
    {
        MuleEvent event = runFlow("jaxwsClient", getTestEvent(TEST_MESSAGE));
        assertOneWayResponse(event);
    }

    @Test
    public void proxyClientSupportsOneWayCall() throws Exception
    {
        String message = "<ns:send xmlns:ns=\"http://testmodels.cxf.module.mule.org/\"><text>hello</text></ns:send>";
        MuleEvent event = runFlow("proxyClient", getTestEvent(message));
        assertOneWayResponse(event);
    }

    private void assertOneWayResponse(MuleEvent event) throws Exception
    {
        assertThat((NullPayload) event.getMessage().getPayload(), is(NullPayload.getInstance()));
        assertThat(event.getMessage().<Integer> getInboundProperty(HTTP_STATUS_PROPERTY), is(ACCEPTED.getStatusCode()));

        AsyncService component = (AsyncService) getComponent("asyncService");
        assertTrue(component.getLatch().await(RECEIVE_TIMEOUT, MILLISECONDS));
    }
}
