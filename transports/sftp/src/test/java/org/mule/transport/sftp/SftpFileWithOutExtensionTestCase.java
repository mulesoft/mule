/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Test;

public class SftpFileWithOutExtensionTestCase extends AbstractSftpFunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "mule-sftp-file-without-extension-config.xml";
    }

    @Override
    protected void setUpTestData() throws IOException
    {
        sftpClient.storeFile("file", new ByteArrayInputStream(TEST_MESSAGE.getBytes()));
    }

    @Test
    public void readsFileWithNoExtension() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.request("vm://testOut", RECEIVE_TIMEOUT);

        assertNotNull("Did not processed the file", response);
    }
}
