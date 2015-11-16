/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ftp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.util.FileUtils;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;

public class FtpEncodingFunctionalTestCase extends AbstractFtpServerTestCase
{

    private static final String TEST_MESSAGE_EUC_JP_ENCODED = "\u3053";
    private static final int FIVE_SECONDS_TIMEOUT = 5000;
    private static final String ENCODING = "EUC-JP";

    private File testDataFile;

    @Override
    protected String getConfigFile()
    {
        return "ftp-encoding-functional-config-flow.xml";
    }

    @Test
    @Ignore("MULE-6926: Flaky Test")
    public void testReadingFileWithEucJpEncodingGetsTheRightText() throws Exception
    {
        File tmpDir = getFtpServerBaseDir();
        testDataFile = createDataFile(tmpDir, ENCODING, TEST_MESSAGE_EUC_JP_ENCODED);

        MuleClient client = muleContext.getClient();
        MuleMessage message = client.request("vm://receive", FIVE_SECONDS_TIMEOUT);

        assertNotNull(message);
        assertEquals(ENCODING, message.getEncoding());
        assertEquals(TEST_MESSAGE_EUC_JP_ENCODED, message.getPayloadAsString());
    }

    @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        FileUtils.deleteTree(testDataFile);
    }
}
