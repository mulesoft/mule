/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.After;
import org.junit.Test;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;

public class SftpEndpointWithRelativePathTestCase extends AbstractSftpFunctionalTestCase
{

    private static final String FILE_CONTENT = "File content";
    private static final String FILE_NAME = "file.txt";

    @Override
    protected String getConfigFile()
    {
        return "mule-sftp-endpoint-with-relative-path-config.xml";
    }

    @Test
    public void writeFileWithRelativePath() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.send("vm://in", FILE_CONTENT, null);
        MuleMessage message = client.request("file://" + TESTDIR + "/" + FILE_NAME, RECEIVE_TIMEOUT);
        assertThat(message.getPayloadAsString(), is(FILE_CONTENT));

    }

    @After
    public void doForceDeleteTestFile() throws Exception
    {
        File f = new File(TESTDIR + "/" + FILE_NAME);
        if (f.exists() && !f.isDirectory())
        {
            f.delete();
        }
    }
}