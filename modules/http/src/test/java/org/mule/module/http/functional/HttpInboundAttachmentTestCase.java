/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.functional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;

import org.junit.Rule;
import org.junit.Test;

public class HttpInboundAttachmentTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort port = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "http-listener-inbound-attachment-config.xml";
    }

    @Test
    public void testStateOfMessageAfterMultiFormRequest() throws Exception
    {
        runFlow("testFlowRequester");
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.request("vm://out", RECEIVE_TIMEOUT);
        assertThat(result.getPayload(), is((Object) NullPayload.getInstance()));
        assertThat(result.getDataType().getType(), is(equalTo((Class)NullPayload.class)));
    }

}
