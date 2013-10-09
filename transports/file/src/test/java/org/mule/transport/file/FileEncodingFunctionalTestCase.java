/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.transport.file.FileTestUtils.createDataFile;
import static org.mule.transport.file.FileTestUtils.createFolder;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class FileEncodingFunctionalTestCase extends AbstractFileFunctionalTestCase
{    
    private static final String TEST_MESSAGE_EUC_JP_ENCODED = "\u3053";
    private static final int FIVE_SECONDS_TIMEOUT = 5000;
    private static final String ENCODING = "EUC-JP";

    public FileEncodingFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);    
    }
    
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "file-encoding-test-service.xml"},
            {ConfigVariant.FLOW, "file-encoding-test-flow.xml"}
        });
    }      

    @Test
    public void testReadingFileWithEucJpEncodingGetsTheRightText() throws Exception
    {
        tmpDir = createFolder(".mule/mule-file-test-EUC-JP");
        createDataFile(tmpDir, TEST_MESSAGE_EUC_JP_ENCODED, ENCODING);

        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.request("vm://receive", FIVE_SECONDS_TIMEOUT);

        assertNotNull(message);
        assertEquals(ENCODING, message.getEncoding());
        assertEquals(TEST_MESSAGE_EUC_JP_ENCODED, message.getPayloadAsString());
    }
}
