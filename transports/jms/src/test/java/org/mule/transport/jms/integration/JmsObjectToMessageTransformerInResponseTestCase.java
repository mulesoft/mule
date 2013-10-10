/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
