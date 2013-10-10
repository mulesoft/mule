/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ftp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.util.FileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class FtpEncodingFunctionalTestCase extends AbstractFtpServerTestCase
{

    private static final String TEST_MESSAGE_EUC_JP_ENCODED = "\u3053";
    private static final int FIVE_SECONDS_TIMEOUT = 5000;
    private static final String ENCODING = "EUC-JP";

    private File testDataFile;

    public FtpEncodingFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "ftp-encoding-functional-config-service.xml"},
            {ConfigVariant.FLOW, "ftp-encoding-functional-config-flow.xml"}
        });
    }      
    
    @Test
    public void testReadingFileWithEucJpEncodingGetsTheRightText() throws Exception
    {
        File tmpDir = new File(FTP_SERVER_BASE_DIR);
        testDataFile = createDataFile(tmpDir, ENCODING, TEST_MESSAGE_EUC_JP_ENCODED);

        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.request("vm://receive", FIVE_SECONDS_TIMEOUT);

        assertNotNull(message);
        assertEquals(ENCODING, message.getEncoding());
        assertEquals(TEST_MESSAGE_EUC_JP_ENCODED, message.getPayloadAsString());
    }

    protected File createDataFile(File folder, String encoding, final String testMessage) throws Exception
    {
        File target = File.createTempFile("mule-file-test-", ".txt", folder);
        target.deleteOnExit();
        FileUtils.writeStringToFile(target, testMessage, encoding);

        return target;
    }

    @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        FileUtils.deleteTree(testDataFile);
    }
}
