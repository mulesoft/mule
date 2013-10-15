/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.integration;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.transport.NullPayload;

import static org.junit.Assert.assertThat;

public class JmsRequestReplyTestCase extends AbstractJmsFunctionalTestCase {

    @Override
    protected String getConfigResources() {
        return "integration/jms-request-reply-config.xml";
    }


    @Test
    public void testJmsWithRequestReply() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("vm://in4jms", "some data", null);
        assertThat(result, IsNull.<Object>notNullValue());
        assertThat(result.getExceptionPayload(), IsNull.<Object>nullValue());
        assertThat(result.getPayload() instanceof NullPayload, Is.is(false));
        assertThat(result.getPayloadAsString(), Is.is("HELLO"));
    }

}
