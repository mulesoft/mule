/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.transport.file.FileTestUtils.createFolder;
import org.mule.tck.junit4.FunctionalTestCase;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class FileEncodingOutboundEndpointFunctionalTestCase extends FunctionalTestCase
{
    private static final String TEST_MESSAGE_EUC_JP_ENCODED = "\u3053";
    private static final String ENCODING = "EUC-JP";

    protected File tmpDir;

    @Override
    protected String getConfigFile()
    {
        return "file-encoding-outbound-endpoint-test-flow.xml";
    }

    @Test
    public void testReadingFileWithEucJpEncodingGetsTheRightText() throws Exception
    {
        tmpDir = createFolder(getFileInsideWorkingDirectory("mule-file-test-EUC-JP").getAbsolutePath());
        runFlow("outputTest");

        File outputFile = getFileInsideWorkingDirectory("mule-file-test-EUC-JP/mule-file-test-EUC-JP");
        assertThat(outputFile.exists(), is(true));
        assertThat(TEST_MESSAGE_EUC_JP_ENCODED, is(IOUtils.toString(new FileInputStream(outputFile), ENCODING)));
    }
}
