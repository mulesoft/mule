/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mule.transport.file.FileTestUtils.createDataFile;

import java.io.File;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.junit.Test;

import org.mule.api.MuleMessage;
import org.mule.tck.junit4.FunctionalTestCase;
import static org.mule.util.FileUtils.openDirectory;

/**
 * When a file connector has the streaming attribute unset,
 * the payload of the message should be a byte array
 * and the file-to-string transformer should be able to
 * process such incoming message.
 * see Jira ticket MULE-11159
 */
public class FileNoStreamingTestCase extends FunctionalTestCase
{

    public FileNoStreamingTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigFile()
    {
        return "file-no-streaming-config.xml";
    }

    @Test
    public void fileToStringNoStreamingFileConnector() throws Exception
    {
        File tmpDir = openDirectory(getFileInsideWorkingDirectory("in").getAbsolutePath());
        createDataFile(tmpDir, TEST_MESSAGE, UTF_8.name());

        muleContext.start();

        MuleMessage response = muleContext.getClient().request("vm://testOut", RECEIVE_TIMEOUT);

        assertThat(TEST_MESSAGE, equalTo(response.getPayload()));
    }

}
