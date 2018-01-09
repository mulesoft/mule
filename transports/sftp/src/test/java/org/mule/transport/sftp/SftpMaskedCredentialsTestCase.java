/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import java.io.ByteArrayInputStream;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.client.MuleClient;
import org.mule.api.processor.MessageProcessor;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mule.tck.AbstractServiceAndFlowTestCase.ConfigVariant.FLOW;

public class SftpMaskedCredentialsTestCase extends AbstractSftpTestCase
{

    private static final int READ_FILE_TIMEOUT = 10000;
    private static String eventToString;

    private static final String FILE_CONTENT = "File content";
    private static final String INBOUND_CREDENTIALS = "inboundCredentials";
    private static final String PASSWORD = "muletest1";

    public SftpMaskedCredentialsTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return asList(new Object[][]{
                {FLOW, "mule-sftp-file-mask-credentials-flow.xml"}});
    }

    @Override
    public void doSetUpBeforeMuleContextCreation() throws Exception
    {
        super.doSetUpBeforeMuleContextCreation();
        sftpClient.mkdir(INBOUND_CREDENTIALS);
    }

    @Override
    protected void doTearDownAfterMuleContextDispose() throws Exception
    {
        super.doTearDownAfterMuleContextDispose();
        sftpClient.changeWorkingDirectory("..");
        sftpClient.recursivelyDeleteDirectory(INBOUND_CREDENTIALS);
        sftpClient.disconnect();
    }

    @Test
    public void credentialsAreMaskedWhenLoggingEvent() throws Exception
    {
        MuleClient muleClient = muleContext.getClient();
        sftpClient.changeWorkingDirectory(INBOUND_CREDENTIALS);
        sftpClient.storeFile(FILENAME, new ByteArrayInputStream(FILE_CONTENT.getBytes()));
        muleClient.request("vm://out.testCredentials", READ_FILE_TIMEOUT);
        assertThat(eventToString, not(containsString(PASSWORD)));
    }

    protected static class EventToStringMessageProcessor implements MessageProcessor
    {

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            eventToString = event.toString();
            return event;
        }

    }

}
