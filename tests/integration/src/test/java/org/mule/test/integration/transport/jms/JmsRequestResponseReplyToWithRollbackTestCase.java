/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
    protected String getConfigFile()
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
