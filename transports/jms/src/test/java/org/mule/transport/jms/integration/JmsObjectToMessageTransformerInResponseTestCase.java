/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.integration;

import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class JmsObjectToMessageTransformerInResponseTestCase extends AbstractJmsFunctionalTestCase
{

    public static final int TIMEOUT = 3000;

    @Override
    protected String getConfigResources()
    {
        return "integration/jms-object-to-message-transformer-test-case.xml";
    }

    @Test
    public void testObjectToMessageDoesntFail() throws Exception
    {
        MuleClient muleClient = new MuleClient(muleContext);
        MuleMessage response = muleClient.send("inWithTransformers", "A message", null, TIMEOUT);
        assertThat(response, IsNull.<Object>notNullValue());
        assertThat(response.getPayloadAsString(), is("A message with something more"));
    }
}
