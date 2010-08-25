/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.util.FileUtils;

import java.io.File;

public class FileEncodingFunctionalTestCase extends AbstractFileFunctionalTestCase
{

    private static final String TEST_MESSAGE_EUC_JP_ENCODED = "\u3053";
    private static final int FIVE_SECONDS_TIMEOUT = 5000;
    private static final String ENCODING = "EUC-JP";

    private File tmpDir;

    @Override
    protected String getConfigResources()
    {
        return "file-encoding-test.xml";
    }

    public void testReadingFileWithEucJpEncodingGetsTheRightText() throws Exception
    {
        tmpDir = createFolder(".mule/mule-file-test-EUC-JP");
        createDataFile(tmpDir, ENCODING, TEST_MESSAGE_EUC_JP_ENCODED);

        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.request("vm://receive", FIVE_SECONDS_TIMEOUT);

        assertNotNull(message);
        assertEquals(ENCODING, message.getEncoding());
        assertEquals(TEST_MESSAGE_EUC_JP_ENCODED, message.getPayloadAsString());
    }

    private File createDataFile(File folder, String encoding, final String testMessage) throws Exception
    {
        File target = File.createTempFile("mule-file-test-", ".txt", folder);
        target.deleteOnExit();
        FileUtils.writeStringToFile(target, testMessage, encoding);

        return target;
    }

    private File createFolder(String name)
    {
        File result = FileUtils.newFile(name);
        result.delete();
        result.mkdir();
        result.deleteOnExit();

        return result;
    }

    @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        FileUtils.deleteTree(tmpDir);
    }
}
