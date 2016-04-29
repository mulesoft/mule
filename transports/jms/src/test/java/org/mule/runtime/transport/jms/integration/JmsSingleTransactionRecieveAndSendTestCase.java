/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.jms.Message;
import javax.jms.TextMessage;

import org.junit.Test;

public class JmsSingleTransactionRecieveAndSendTestCase extends AbstractJmsFunctionalTestCase
{
    @Override
    protected String getConfigFile()
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
