/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp.reliability;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.processor.MessageProcessor;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class SftpRedeliveryPolicyTestCase extends AbstractSftpRedeliveryTestCase
{

    private static final String FAIL = "fail";
    private static CountDownLatch latch = new CountDownLatch(3);

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> parameters()
    {
        return getParameters();
    }

    public SftpRedeliveryPolicyTestCase(String name, boolean archive)
    {
        super(name, archive);
    }

    @Override
    protected String getConfigFile()
    {
        return "sftp-redelivery-config.xml";
    }

    @Test
    public void testDeadLetterQueueDelivery() throws Exception
    {
        sftpClient.storeFile(FILENAME, new ByteArrayInputStream(FAIL.getBytes()));
        latch.await(TIMEOUT, TimeUnit.MILLISECONDS);
        MuleClient muleClient = muleContext.getClient();
        MuleMessage response = muleClient.request("vm://error-queue", TIMEOUT);

        assertThat(response, notNullValue());
        assertThat(response.getPayloadAsString(), is(FAIL));
        assertFilesDeleted();
    }

    public static class CountDownAndFailMessageProcessor implements MessageProcessor
    {

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            String payload = null;
            try
            {
                payload = event.getMessage().getPayloadAsString();
            }
            catch (Exception e)
            {
                //do nothing
            }
            if (FAIL.equals(payload))
            {
                latch.countDown();
                throw new RuntimeException();
            }
            return event;
        }
    }

}
