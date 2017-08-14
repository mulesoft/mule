/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.pgp;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Prober;
import org.mule.tck.probe.file.FileDoesNotExists;
import org.mule.tck.probe.file.FileExists;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class FileDecryptionTestCase extends FunctionalTestCase
{
    public String testFolder;

    private Prober prober = new PollingProber(30000, 100);

    public FileDecryptionTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigFile()
    {
        return "file-decryption-config.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        testFolder = getFileInsideWorkingDirectory("testData").getAbsolutePath();
    }

    private File createTestFile(String folder) throws IOException
    {
        File testFolder = new File(folder);
        testFolder.mkdirs();
        prober.check(new FileExists(testFolder));

        File target = File.createTempFile("mule-file-test-", ".txt", testFolder);
        target.deleteOnExit();
        FileUtils.writeStringToFile(target, "TEST");
        prober.check(new FileExists(target));

        return target;
    }

    @Test
    public void decryptsFile() throws Exception
    {
        final File target = createTestFile(testFolder);

        // Starts file endpoint polling
        muleContext.start();

        final File encryptedFile = getFile("encrypted", target.getName());
        prober.check(new FileDoesNotExists(encryptedFile));

        final File decryptedFile = getFile("decrypted", target.getName());
        prober.check(new FileExists(decryptedFile));

    }

    private File getFile(String folderName, String fileName)
    {
        final File subFolder = new File(testFolder, folderName);
        return new File(subFolder, fileName);
    }
}
