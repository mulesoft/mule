/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.core.env.AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.util.concurrent.Latch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class SftpAutoDeleteTestCase extends AbstractSftpFunctionalTestCase
{

    private static final String FILENAME = "file.txt";
    private static final String FILE_CONTENT = "File content";
    private static final String AUTO_DELETE_OFF = "autoDeleteOff";
    private static final String AUTO_DELETE_ON = "autoDeleteOn";
    private static final int TIMEOUT = 1500;

    private static Latch latch;
    private static MuleMessage  message;

    @Rule
    public SystemProperty profile;

    @Parameterized.Parameters(name= "{0}")
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"explicit-connector"},
                {"implicit-connector"}});
    }

    public SftpAutoDeleteTestCase(String profile)
    {
        this.profile = new SystemProperty(ACTIVE_PROFILES_PROPERTY_NAME, profile);
    }

    @Override
    protected String getConfigFile()
    {
        return "mule-sftp-auto-delete-config.xml";
    }

    @Override
    protected void setUpTestData() throws IOException
    {
        latch = new Latch();
        sftpClient.mkdir(AUTO_DELETE_OFF);
        sftpClient.mkdir(AUTO_DELETE_ON);
    }

    @Override
    public void doTearDownAfterMuleContextDispose() throws Exception
    {
        sftpClient.changeWorkingDirectory("..");
        sftpClient.recursivelyDeleteDirectory(AUTO_DELETE_OFF);
        sftpClient.recursivelyDeleteDirectory(AUTO_DELETE_ON);
        super.doTearDownAfterMuleContextDispose();
    }

    @Test
    public void endpointAutoDeleteFalse() throws Exception
    {
        testDirectory(AUTO_DELETE_OFF);
        assertThat(Arrays.asList(sftpClient.listFiles()), hasItem(FILENAME));
    }

    @Test
    public void endpointAutoDeleteTrue() throws Exception
    {
        testDirectory(AUTO_DELETE_ON);
        assertThat(Arrays.asList(sftpClient.listFiles()), is(empty()));
    }

    private void testDirectory(String directory) throws Exception
    {
        sftpClient.changeWorkingDirectory(directory);
        sftpClient.storeFile(FILENAME, new ByteArrayInputStream(FILE_CONTENT.getBytes()));
        latch.await(TIMEOUT, TimeUnit.MILLISECONDS);
        assertThat(message, notNullValue());
        assertThat(message.getPayloadAsString(), is(FILE_CONTENT));
    }

    public static class LatchMessageProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            message = event.getMessage();
            latch.release();
            return event;
        }
    }
}
