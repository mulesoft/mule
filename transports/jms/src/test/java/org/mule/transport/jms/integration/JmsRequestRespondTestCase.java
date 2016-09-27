/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.integration;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.tck.functional.FlowAssert.verify;
import org.mule.api.MuleMessage;

import org.junit.Test;

public class JmsRequestRespondTestCase extends AbstractJmsFunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "integration/jms-request-response.xml";
    }


    @Test
    public void testNotSendingCorrelationIDWhenIsNotSettedUpTemporaryQueue() throws Exception
    {
        MuleMessage response = runFlow("JMSNoCorrelationIDTemporaryQueue", TEST_MESSAGE).getMessage();
        verify("JMSNoCorrelationIDTemporaryQueue");
        verify("JMSInNoCorrelationID");
        assertEchoResponse(response);
    }

    @Test
    public void testNotSendingCorrelationIDWhenIsNotSettedUpFixedQueue() throws Exception
    {
        MuleMessage response = runFlow("JMSNoCorrelationIDFixedQueue", TEST_MESSAGE).getMessage();
        verify("JMSNoCorrelationIDFixedQueue");
        verify("JMSInNoCorrelationID");
        assertEchoResponse(response);
    }

    @Test
    public void testSendingCorrelationIDWhenIsSettedUpTemporaryQueue() throws Exception
    {
        MuleMessage response = runFlow("JMSCorrelationIDTemporaryQueue", TEST_MESSAGE).getMessage();
        verify("JMSCorrelationIDTemporaryQueue");
        verify("JMSInCustomCorrelationID");
        assertFixedEchoResponse(response);
    }

    @Test
    public void testSendingCorrelationIDWhenIsSettedUpFixedQueue() throws Exception
    {
        MuleMessage response = runFlow("JMSCorrelationIDFixedQueue", TEST_MESSAGE).getMessage();
        verify("JMSCorrelationIDFixedQueue");
        verify("JMSInCustomCorrelationID");
        assertFixedEchoResponse(response);
    }

    private void assertEchoResponse(MuleMessage response) throws Exception
    {
        assertThat(response.getPayloadAsString(), equalTo(TEST_MESSAGE + " " + "JMSInNoCorrelationID"));
    }

    private void assertFixedEchoResponse(MuleMessage response) throws Exception
    {
        assertThat(response.getPayloadAsString(), equalTo(TEST_MESSAGE + " " + "JMSInCustomCorrelationID"));
    }
}
