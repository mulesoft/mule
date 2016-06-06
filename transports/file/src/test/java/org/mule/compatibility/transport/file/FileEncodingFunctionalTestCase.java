/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.compatibility.transport.file.FileTestUtils.createDataFile;
import static org.mule.compatibility.transport.file.FileTestUtils.createFolder;

import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import org.junit.Test;

public class FileEncodingFunctionalTestCase extends AbstractFileFunctionalTestCase
{
    private static final String TEST_MESSAGE_EUC_JP_ENCODED = "\u3053";
    private static final int FIVE_SECONDS_TIMEOUT = 5000;
    private static final String ENCODING = "EUC-JP";


    @Override
    protected String getConfigFile()
    {
        return "file-encoding-test-flow.xml";
    }

    @Test
    public void testReadingFileWithEucJpEncodingGetsTheRightText() throws Exception
    {
        tmpDir = createFolder(getFileInsideWorkingDirectory("mule-file-test-EUC-JP").getAbsolutePath());
        createDataFile(tmpDir, TEST_MESSAGE_EUC_JP_ENCODED, ENCODING);

        MuleClient client = muleContext.getClient();
        MuleMessage message = client.request("vm://receive", FIVE_SECONDS_TIMEOUT);

        assertNotNull(message);
        assertEquals(ENCODING, message.getEncoding());
        assertEquals(TEST_MESSAGE_EUC_JP_ENCODED, getPayloadAsString(message));
    }
}
