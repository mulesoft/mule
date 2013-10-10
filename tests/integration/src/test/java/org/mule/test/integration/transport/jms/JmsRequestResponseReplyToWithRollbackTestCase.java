/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.transport.jms;

import static org.junit.Assert.assertEquals;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class JmsRequestResponseReplyToWithRollbackTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/transport/jms/jms-request-response-reply-to-with-rollback-config.xml";
    }

    @Test
    public void maintainsPayloadSetInRollbackExceptionStrategy() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("jms://testIn", TEST_MESSAGE, null);

        assertEquals("ROLLBACK", response.getPayloadAsString());
    }
}
