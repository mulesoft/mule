/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.construct.Flow;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * <code>SftpFileAgeFunctionalTestCase</code> tests the fileAge functionality.
 *
 * @author Lennart Häggkvist
 */

public class SftpFileAgeFunctionalTestCase extends AbstractSftpTestCase
{
    private static final String FILE_CONTENT = "File content";
    private static final String INBOUND_HIGH_AGE = "inboundHighAge";
    private static final String INBOUND_LOW_AGE = "inboundLowAge";
    protected static final long TIMEOUT = 10000;
    private static final long READ_FILE_TIMEOUT = 2000;

    public SftpFileAgeFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
         {ConfigVariant.SERVICE, "mule-sftp-file-age-config-service.xml"},
        {ConfigVariant.FLOW, "mule-sftp-file-age-config-flow.xml"}});
    }

    @Override
    public void doSetUpBeforeMuleContextCreation() throws Exception
    {
        super.doSetUpBeforeMuleContextCreation();
        sftpClient.mkdir(INBOUND_LOW_AGE);
        sftpClient.mkdir(INBOUND_HIGH_AGE);
    }

    @Override
    protected void doTearDownAfterMuleContextDispose() throws Exception
    {
        super.doTearDownAfterMuleContextDispose();
        sftpClient.changeWorkingDirectory("..");
        sftpClient.recursivelyDeleteDirectory(INBOUND_HIGH_AGE);
        sftpClient.recursivelyDeleteDirectory(INBOUND_LOW_AGE);
        sftpClient.disconnect();
    }

    @Test
    public void doesNotProcessFileYoungerThanFileAge() throws Exception
    {
        MuleClient muleClient = muleContext.getClient();
        sftpClient.changeWorkingDirectory(INBOUND_HIGH_AGE);
        sftpClient.storeFile(FILENAME, new ByteArrayInputStream(FILE_CONTENT.getBytes()));
        assertNull(muleClient.request("vm://out.higAge", READ_FILE_TIMEOUT));
        assertTrue(Arrays.asList(sftpClient.listFiles()).contains(FILENAME));
    }

    @Test
    public void processFileOlderThanFileAge() throws Exception
    {
        MuleClient muleClient = muleContext.getClient();
        sftpClient.changeWorkingDirectory(INBOUND_LOW_AGE);
        sftpClient.storeFile(FILENAME, new ByteArrayInputStream(FILE_CONTENT.getBytes()));
        MuleMessage message = muleClient.request("vm://out.lowAge", READ_FILE_TIMEOUT);
        assertNotNull(message);
        assertEquals(FILE_CONTENT, message.getPayloadAsString());
        assertFalse(Arrays.asList(sftpClient.listFiles()).contains(FILENAME));
    }

}
