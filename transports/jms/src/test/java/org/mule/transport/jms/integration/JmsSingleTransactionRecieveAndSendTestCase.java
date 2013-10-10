/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.integration;

import javax.jms.Message;
import javax.jms.TextMessage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JmsSingleTransactionRecieveAndSendTestCase extends AbstractJmsFunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "integration/jms-single-tx-receive-send-in-one-tx.xml";
    }

    @Test
    public void testSingleTransactionBeginOrJoinAndAlwaysBegin() throws Exception
    {
        send(scenarioCommit);
        Message message = receive(scenarioReceive);
        assertNotNull(message);
        assertTrue(TextMessage.class.isAssignableFrom(message.getClass()));
        assertEquals(((TextMessage) message).getText(), AbstractJmsFunctionalTestCase.DEFAULT_OUTPUT_MESSAGE);
    }

}
