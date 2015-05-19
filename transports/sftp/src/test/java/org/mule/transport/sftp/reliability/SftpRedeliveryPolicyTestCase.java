/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp.reliability;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.builders.SimpleConfigurationBuilder;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.transport.sftp.AbstractSftpFunctionalTestCase;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SftpRedeliveryPolicyTestCase extends AbstractSftpFunctionalTestCase
{

    private static final String FILENAME = "file.txt";
    private static final int TIMEOUT = 300000;
    private static final String FAIL = "fail";
    private static CountDownLatch latch = new CountDownLatch(3);

    @Parameters(name = "{0}")
    public static List<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]
                                     {
                                             {"Without Archiving", false},
                                             {"With Archiving", true}
                                     });
    }

    private final String name;
    private final boolean archive;

    @Rule
    public TemporaryFolder archiveFolder = new TemporaryFolder();

    @Rule
    public SystemProperty archiveProperty;

    public SftpRedeliveryPolicyTestCase(String name, boolean archive)
    {
        this.name = name;
        this.archive = archive;
    }

    @Override
    protected void addBuilders(List<ConfigurationBuilder> builders)
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("archiveDir", archive ? archiveFolder.getRoot().getAbsolutePath() : "");

        builders.add(0, new SimpleConfigurationBuilder(properties));
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
    }

    @Test
    public void testSuccessfulDelivery() throws Exception
    {
        sftpClient.storeFile(FILENAME, new ByteArrayInputStream(TEST_PAYLOAD.getBytes()));
        MuleClient muleClient = muleContext.getClient();
        MuleMessage response = muleClient.request("vm://out", TIMEOUT);

        assertThat(response, notNullValue());
        assertThat(response.getPayloadAsString(), is(TEST_PAYLOAD));
        assertThat(Arrays.asList(sftpClient.listFiles()), is(empty()));
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
